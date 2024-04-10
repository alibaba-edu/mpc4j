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
import edu.alibaba.mpc4j.s2pc.opf.oprp.AbstractOprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSenderOutput;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LowMc-OPRP协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class LowMcOprpSender extends AbstractOprpSender {
    /**
     * Z2 circuit sender
     */
    private final Z2cParty z2cSender;
    /**
     * 初始变换密钥
     */
    private long[] initKeyShare;
    /**
     * 轮密钥取值，一共有r组，每组为128比特的布尔元素
     */
    private long[][] roundKeyShares;

    public LowMcOprpSender(Rpc senderRpc, Party receiverParty, LowMcOprpConfig config) {
        super(LowMcOprpPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cSender);
    }

    public LowMcOprpSender(Rpc senderRpc, Party receiverParty, Party aiderParty, LowMcOprpConfig config) {
        super(LowMcOprpPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, aiderParty, config.getZ2cConfig());
        addSubPto(z2cSender);
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
        z2cSender.init(LowMcUtils.SBOX_NUM * 3 * maxRoundBatchSize * LowMcUtils.ROUND);
        stopWatch.stop();
        long initBcTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initBcTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OprpSenderOutput oprp(byte[] key, int batchSize) throws MpcAbortException {
        setPtoInput(key, batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[] senderShareKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(senderShareKeyBytes);
        byte[] receiverShareKeyBytes = BytesUtils.xor(key, senderShareKeyBytes);
        List<byte[]> shareKeyDataPacket = new LinkedList<>();
        shareKeyDataPacket.add(receiverShareKeyBytes);
        DataPacketHeader shareKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SHARE_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(shareKeyHeader, shareKeyDataPacket));
        stopWatch.stop();
        long shareKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, shareKeyTime);

        stopWatch.start();
        extendKey(senderShareKeyBytes);
        stopWatch.stop();
        long extendKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, extendKeyTime);

        stopWatch.start();
        DataPacketHeader shareMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SHARE_MESSAGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> shareMessagePayload = rpc.receive(shareMessageHeader).getPayload();
        MpcAbortPreconditions.checkArgument(shareMessagePayload.size() == batchSize);
        long[][] stateLongs = shareMessagePayload.stream()
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
            // state = state + Constants(i)
            addConstants(stateLongs, roundIndex);
            // generate round key and add to the state
            addRoundKeys(stateLongs, roundIndex);
        }
        // ciphertext = state
        byte[][] shares = Arrays.stream(stateLongs)
            .map(LongUtils::longArrayToByteArray)
            .toArray(byte[][]::new);
        OprpSenderOutput senderOutput = new OprpSenderOutput(PrpType.JDK_LONGS_LOW_MC_20, false, key, shares);
        stopWatch.stop();
        long oprpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, oprpTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void extendKey(byte[] senderShareKeyBytes) {
        long[] senderShareKeyLongs = LongUtils.byteArrayToLongArray(senderShareKeyBytes);
        // 初始扩展密钥
        initKeyShare = LowMcUtils.KEY_MATRICES[0].leftMultiply(senderShareKeyLongs);
        // 根据轮数扩展密钥
        roundKeyShares = IntStream.range(0, LowMcUtils.ROUND)
            .mapToObj(roundIndex -> LowMcUtils.KEY_MATRICES[roundIndex + 1].leftMultiply(senderShareKeyLongs))
            .toArray(long[][]::new);
    }

    private void addInitKeys(long[][] stateLongs) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(stateLongs[row], initKeyShare));
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
        TransBitMatrix stateBytesTransposeMatrix = stateBytesTransBitMatrix.transpose();
        // 创建sbox后的转置矩阵
        TransBitMatrix sboxStateBytesTransMatrix = TransBitMatrixFactory.createInstance(
            envType, batchSize, CommonConstants.BLOCK_BIT_LENGTH, parallel
        );
        // 复制sbox后的列
        for (int columnIndex = LowMcUtils.SBOX_NUM * 3; columnIndex < CommonConstants.BLOCK_BIT_LENGTH; columnIndex++) {
            sboxStateBytesTransMatrix.setColumn(columnIndex, stateBytesTransposeMatrix.getColumn(columnIndex));
        }
        // sbox处理
        byte[] baa0 = new byte[LowMcUtils.SBOX_NUM * 3 * batchByteSize];
        byte[] ccb0 = new byte[LowMcUtils.SBOX_NUM * 3 * batchByteSize];
        for (int sboxIndex = 0; sboxIndex < LowMcUtils.SBOX_NUM; sboxIndex++) {
            int offset = 3 * batchByteSize * sboxIndex;
            byte[] a0 = stateBytesTransposeMatrix.getColumn(sboxIndex * 3);
            byte[] b0 = stateBytesTransposeMatrix.getColumn(sboxIndex * 3 + 1);
            byte[] c0 = stateBytesTransposeMatrix.getColumn(sboxIndex * 3 + 2);
            System.arraycopy(b0, 0, baa0, offset, batchByteSize);
            System.arraycopy(a0, 0, baa0, offset + batchByteSize, batchByteSize);
            System.arraycopy(a0, 0, baa0, offset + 2 * batchByteSize, batchByteSize);
            System.arraycopy(c0, 0, ccb0, offset, batchByteSize);
            System.arraycopy(c0, 0, ccb0, offset + batchByteSize, batchByteSize);
            System.arraycopy(b0, 0, ccb0, offset + 2 * batchByteSize, batchByteSize);
        }
        // 一轮AND运算
        byte[] sbox0 = z2cSender.and(
            SquareZ2Vector.create(LowMcUtils.SBOX_NUM * 3 * roundBatchSize, baa0, false),
            SquareZ2Vector.create(LowMcUtils.SBOX_NUM * 3 * roundBatchSize, ccb0, false)
        ).getBitVector().getBytes();
        // 拆分结果
        for (int sboxIndex = 0; sboxIndex < LowMcUtils.SBOX_NUM; sboxIndex++) {
            int offset = 3 * batchByteSize * sboxIndex;
            byte[] a0 = stateBytesTransposeMatrix.getColumn(sboxIndex * 3);
            byte[] b0 = stateBytesTransposeMatrix.getColumn(sboxIndex * 3 + 1);
            byte[] c0 = stateBytesTransposeMatrix.getColumn(sboxIndex * 3 + 2);
            // a = a ⊕ (b ☉ c)
            byte[] bc0 = new byte[batchByteSize];
            System.arraycopy(sbox0, offset, bc0, 0, batchByteSize);
            BytesUtils.reduceByteArray(bc0, batchSize);
            SquareZ2Vector a0Sbox = z2cSender.xor(
                SquareZ2Vector.create(batchSize, a0, false),
                SquareZ2Vector.create(batchSize, bc0, false)
            );
            byte[] a0SboxBytes = a0Sbox.getBitVector().getBytes();
            // b = a ⊕ b ⊕ (a ☉ c)
            byte[] ac0 = new byte[batchByteSize];
            System.arraycopy(sbox0, offset + batchByteSize, ac0, 0, batchByteSize);
            BytesUtils.reduceByteArray(ac0, batchSize);
            SquareZ2Vector b0Sbox = z2cSender.xor(
                SquareZ2Vector.create(batchSize, a0, false),
                SquareZ2Vector.create(batchSize, b0, false)
            );
            b0Sbox = z2cSender.xor(b0Sbox, SquareZ2Vector.create(batchSize, ac0, false));
            byte[] b0SboxBytes = b0Sbox.getBitVector().getBytes();
            // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
            byte[] ab0 = new byte[batchByteSize];
            System.arraycopy(sbox0, offset + 2 * batchByteSize, ab0, 0, batchByteSize);
            BytesUtils.reduceByteArray(ab0, batchSize);
            SquareZ2Vector c0Sbox = z2cSender.xor(
                SquareZ2Vector.create(batchSize, a0, false),
                SquareZ2Vector.create(batchSize, b0, false)
            );
            c0Sbox = z2cSender.xor(c0Sbox, SquareZ2Vector.create(batchSize, c0, false));
            c0Sbox = z2cSender.xor(c0Sbox, SquareZ2Vector.create(batchSize, ab0, false));
            byte[] c0SboxBytes = c0Sbox.getBitVector().getBytes();
            sboxStateBytesTransMatrix.setColumn(sboxIndex * 3, a0SboxBytes);
            sboxStateBytesTransMatrix.setColumn(sboxIndex * 3 + 1, b0SboxBytes);
            sboxStateBytesTransMatrix.setColumn(sboxIndex * 3 + 2, c0SboxBytes);
        }
        TransBitMatrix sboxStateBytesTransBitMatrix = sboxStateBytesTransMatrix.transpose();
        for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
            stateLongs[batchIndex] = LongUtils.byteArrayToLongArray(sboxStateBytesTransBitMatrix.getColumn(batchIndex));
        }
    }

    private long[][] linearTransforms(long[][] states, int roundIndex) {
        IntStream rowIndexIntStream = IntStream.range(0, batchSize);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        return rowIndexIntStream
            .mapToObj(row -> LowMcUtils.LINEAR_MATRICES[roundIndex].leftMultiply(states[row]))
            .toArray(long[][]::new);
    }

    private void addConstants(long[][] states, int roundIndex) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(states[row], LowMcUtils.CONSTANTS[roundIndex]));
    }

    private void addRoundKeys(long[][] states, int roundIndex) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(states[row], roundKeyShares[roundIndex]));
    }
}
