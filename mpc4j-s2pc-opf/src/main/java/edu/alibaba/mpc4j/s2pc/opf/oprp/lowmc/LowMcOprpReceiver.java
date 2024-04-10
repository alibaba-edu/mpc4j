package edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.oprp.AbstractOprpReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LowMc-OPRP协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class LowMcOprpReceiver extends AbstractOprpReceiver {
    /**
     * Z2 circuit receiver
     */
    private final Z2cParty z2cReceiver;
    /**
     * 初始变换密钥
     */
    private long[] initKeyShare;
    /**
     * 轮密钥取值，一共有r组，每组为128比特的布尔元素
     */
    private long[][] roundKeyShares;

    public LowMcOprpReceiver(Rpc receiverRpc, Party senderParty, LowMcOprpConfig config) {
        super(LowMcOprpPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
    }

    public LowMcOprpReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, LowMcOprpConfig config) {
        super(LowMcOprpPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, aiderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
    }

    @Override
    public PrpType getPrpType() {
        return PrpType.JDK_LONGS_LOW_MC_20;
    }

    @Override
    public boolean isInvPrp() {
        return false;
    }

    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化BC协议
        z2cReceiver.init(LowMcUtils.SBOX_NUM * 3 * maxRoundBatchSize * LowMcUtils.ROUND);
        stopWatch.stop();
        long initBcTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initBcTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OprpReceiverOutput oprp(byte[][] messages) throws MpcAbortException {
        setPtoInput(messages);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] receiverShareMessages = new byte[batchSize][CommonConstants.BLOCK_BYTE_LENGTH];
        List<byte[]> shareMessagePayload = IntStream.range(0, batchSize)
            .mapToObj(index -> {
                secureRandom.nextBytes(receiverShareMessages[index]);
                return BytesUtils.xor(messages[index], receiverShareMessages[index]);
            })
            .collect(Collectors.toList());
        DataPacketHeader shareMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), LowMcOprpPtoDesc.PtoStep.CLIENT_SEND_SHARE_MESSAGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(shareMessageHeader, shareMessagePayload));
        stopWatch.stop();
        long shareMessageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, shareMessageTime);

        stopWatch.start();
        DataPacketHeader shareKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), LowMcOprpPtoDesc.PtoStep.SERVER_SEND_SHARE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> shareKeyPayload = rpc.receive(shareKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(shareKeyPayload.size() == 1);
        byte[] receiverShareKeyBytes = shareKeyPayload.remove(0);
        extendKey(receiverShareKeyBytes);
        stopWatch.stop();
        long extendKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, extendKeyTime);

        stopWatch.start();
        long[][] stateLongs = Arrays.stream(receiverShareMessages)
            .map(LongUtils::byteArrayToLongArray)
            .toArray(long[][]::new);
        stopWatch.stop();
        long convertMessageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, convertMessageTime);

        stopWatch.start();
        // initial whitening
        // state = plaintext + MultiplyWithGF2Matrix(KMatrix(0),key)
        addInitKeys(stateLongs);
        for (int roundIndex = 0; roundIndex < LowMcUtils.ROUND; roundIndex++) {
            // m computations of 3-bit sbox, remaining n-3m bits remain the same
            sboxLayer(stateLongs);
            // affine layer, state = MultiplyWithGF2Matrix(LMatrix(i),state)
            stateLongs = linearTransforms(stateLongs, roundIndex);
            // state = state + Constants(i)，安全计算状态下不需要加常数
            // generate round key and add to the state
            addRoundKeys(stateLongs, roundIndex);
        }
        // ciphertext = state
        byte[][] shares = Arrays.stream(stateLongs)
            .map(LongUtils::longArrayToByteArray)
            .toArray(byte[][]::new);
        OprpReceiverOutput receiverOutput = new OprpReceiverOutput(PrpType.JDK_LONGS_LOW_MC_20, false, shares);
        stopWatch.stop();
        long oprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, oprpTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void extendKey(byte[] receiverShareKeyBytes) {
        long[] receiverShareKeyLongs = LongUtils.byteArrayToLongArray(receiverShareKeyBytes);
        // 初始扩展密钥
        initKeyShare = LowMcUtils.KEY_MATRICES[0].leftMultiply(receiverShareKeyLongs);
        // 根据轮数扩展密钥
        roundKeyShares = IntStream.range(0, LowMcUtils.ROUND)
            .mapToObj(roundIndex -> LowMcUtils.KEY_MATRICES[roundIndex + 1].leftMultiply(receiverShareKeyLongs))
            .toArray(long[][]::new);
    }

    private void addInitKeys(long[][] state) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(state[row], initKeyShare));
    }

    private void sboxLayer(long[][] stateLongs) throws MpcAbortException {
        byte[][] stateBytes = Arrays.stream(stateLongs)
            .map(LongUtils::longArrayToByteArray)
            .toArray(byte[][]::new);
        TransBitMatrix stateBytesTransBitMatrix = TransBitMatrixFactory.createInstance(
            envType, CommonConstants.BLOCK_BIT_LENGTH, batchSize, parallel
        );
        for (int i = 0; i < batchSize; i++) {
            stateBytesTransBitMatrix.setColumn(i, stateBytes[i]);
        }
        TransBitMatrix stateBytesTransMatrix = stateBytesTransBitMatrix.transpose();
        // 创建sbox后的转置矩阵
        TransBitMatrix sboxStateBytesTransMatrix = TransBitMatrixFactory.createInstance(
            envType, batchSize, CommonConstants.BLOCK_BIT_LENGTH, parallel
        );
        // 复制sbox后的列
        for (int columnIndex = LowMcUtils.SBOX_NUM * 3; columnIndex < CommonConstants.BLOCK_BIT_LENGTH; columnIndex++) {
            sboxStateBytesTransMatrix.setColumn(columnIndex, stateBytesTransMatrix.getColumn(columnIndex));
        }
        // sbox处理
        byte[] baa1 = new byte[LowMcUtils.SBOX_NUM * 3 * batchByteSize];
        byte[] ccb1 = new byte[LowMcUtils.SBOX_NUM * 3 * batchByteSize];
        for (int sboxIndex = 0; sboxIndex < LowMcUtils.SBOX_NUM; sboxIndex++) {
            int offset = 3 * batchByteSize * sboxIndex;
            byte[] a1 = stateBytesTransMatrix.getColumn(sboxIndex * 3);
            byte[] b1 = stateBytesTransMatrix.getColumn(sboxIndex * 3 + 1);
            byte[] c1 = stateBytesTransMatrix.getColumn(sboxIndex * 3 + 2);
            System.arraycopy(b1, 0, baa1, offset, batchByteSize);
            System.arraycopy(a1, 0, baa1, offset + batchByteSize, batchByteSize);
            System.arraycopy(a1, 0, baa1, offset + 2 * batchByteSize, batchByteSize);
            System.arraycopy(c1, 0, ccb1, offset, batchByteSize);
            System.arraycopy(c1, 0, ccb1, offset + batchByteSize, batchByteSize);
            System.arraycopy(b1, 0, ccb1, offset + 2 * batchByteSize, batchByteSize);
        }
        // 一轮AND运算
        byte[] sbox1 = z2cReceiver.and(
            SquareZ2Vector.create(LowMcUtils.SBOX_NUM * 3 * roundBatchSize, baa1, false),
            SquareZ2Vector.create(LowMcUtils.SBOX_NUM * 3 * roundBatchSize, ccb1, false)
        ).getBitVector().getBytes();
        for (int sboxIndex = 0; sboxIndex < LowMcUtils.SBOX_NUM; sboxIndex++) {
            int offset = 3 * batchByteSize * sboxIndex;
            byte[] a1 = stateBytesTransMatrix.getColumn(sboxIndex * 3);
            byte[] b1 = stateBytesTransMatrix.getColumn(sboxIndex * 3 + 1);
            byte[] c1 = stateBytesTransMatrix.getColumn(sboxIndex * 3 + 2);
            // a = a ⊕ (b ☉ c)
            byte[] bc1 = new byte[batchByteSize];
            System.arraycopy(sbox1, offset, bc1, 0, batchByteSize);
            BytesUtils.reduceByteArray(bc1, batchSize);
            SquareZ2Vector a1Sbox = z2cReceiver.xor(
                SquareZ2Vector.create(batchSize, a1, false),
                SquareZ2Vector.create(batchSize, bc1, false)
            );
            byte[] a1SboxBytes = a1Sbox.getBitVector().getBytes();
            // b = a ⊕ b ⊕ (a ☉ c)
            byte[] ac1 = new byte[batchByteSize];
            System.arraycopy(sbox1, offset + batchByteSize, ac1, 0, batchByteSize);
            BytesUtils.reduceByteArray(ac1, batchSize);
            SquareZ2Vector b1Sbox = z2cReceiver.xor(
                SquareZ2Vector.create(batchSize, a1, false),
                SquareZ2Vector.create(batchSize, b1, false)
            );
            b1Sbox = z2cReceiver.xor(b1Sbox, SquareZ2Vector.create(batchSize, ac1, false));
            byte[] b1SboxBytes = b1Sbox.getBitVector().getBytes();
            // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
            byte[] ab1 = new byte[batchByteSize];
            System.arraycopy(sbox1, offset + 2 * batchByteSize, ab1, 0, batchByteSize);
            BytesUtils.reduceByteArray(ab1, batchSize);
            SquareZ2Vector c1Sbox = z2cReceiver.xor(
                SquareZ2Vector.create(batchSize, a1, false),
                SquareZ2Vector.create(batchSize, b1, false)
            );
            c1Sbox = z2cReceiver.xor(c1Sbox, SquareZ2Vector.create(batchSize, c1, false));
            c1Sbox = z2cReceiver.xor(c1Sbox, SquareZ2Vector.create(batchSize, ab1, false));
            byte[] c1SboxBytes = c1Sbox.getBitVector().getBytes();
            stateBytesTransMatrix.setColumn(sboxIndex * 3, a1SboxBytes);
            stateBytesTransMatrix.setColumn(sboxIndex * 3 + 1, b1SboxBytes);
            stateBytesTransMatrix.setColumn(sboxIndex * 3 + 2, c1SboxBytes);
        }
        TransBitMatrix sboxStateBytesTransBitMatrix = stateBytesTransMatrix.transpose();
        for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
            stateLongs[batchIndex] = LongUtils.byteArrayToLongArray(sboxStateBytesTransBitMatrix.getColumn(batchIndex));
        }
    }

    private long[][] linearTransforms(long[][] stateLongs, int roundIndex) {
        IntStream rowIndexIntStream = IntStream.range(0, batchSize);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        return rowIndexIntStream
            .mapToObj(row -> LowMcUtils.LINEAR_MATRICES[roundIndex].leftMultiply(stateLongs[row]))
            .toArray(long[][]::new);
    }

    private void addRoundKeys(long[][] stateLongs, int roundIndex) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(stateLongs[row], roundKeyShares[roundIndex]));
    }
}
