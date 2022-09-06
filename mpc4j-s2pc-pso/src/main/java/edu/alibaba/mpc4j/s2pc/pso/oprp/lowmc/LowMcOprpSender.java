package edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.pso.oprp.AbstractOprpSender;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpSenderOutput;

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
     * BC协议发送方
     */
    private final BcParty bcSender;
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
        bcSender = BcFactory.createSender(senderRpc, receiverParty, config.getBcConfig());
        bcSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        bcSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        bcSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        bcSender.addLogLevel();
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
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化BC协议
        bcSender.init(
            LowMcUtils.SBOX_NUM * 3 * maxRoundBatchSize,
            LowMcUtils.SBOX_NUM * 3 * maxRoundBatchSize * LowMcUtils.ROUND
        );
        stopWatch.stop();
        long initBcTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initBcTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public OprpSenderOutput oprp(byte[] key, int batchSize) throws MpcAbortException {
        setPtoInput(key, batchSize);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[] senderShareKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(senderShareKeyBytes);
        byte[] receiverShareKeyBytes = BytesUtils.xor(key, senderShareKeyBytes);
        List<byte[]> shareKeyDataPacket = new LinkedList<>();
        shareKeyDataPacket.add(receiverShareKeyBytes);
        DataPacketHeader shareKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), LowMcOprpPtoDesc.PtoStep.SERVER_SEND_SHARE_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(shareKeyHeader, shareKeyDataPacket));
        stopWatch.stop();
        long shareKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), shareKeyTime);

        stopWatch.start();
        extendKey(senderShareKeyBytes);
        stopWatch.stop();
        long extendKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), extendKeyTime);

        stopWatch.start();
        DataPacketHeader shareMessageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), LowMcOprpPtoDesc.PtoStep.CLIENT_SEND_SHARE_MESSAGE.ordinal(), extraInfo,
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
        info("{}{} Send. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), convertMessageTime);

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
        info("{}{} Send. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprpTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void extendKey(byte[] senderShareKeyBytes) {
        long[] senderShareKeyLongs = LongUtils.byteArrayToLongArray(senderShareKeyBytes);
        // 初始扩展密钥
        initKeyShare = LowMcUtils.KEY_MATRICES[0].lmul(senderShareKeyLongs);
        // 根据轮数扩展密钥
        roundKeyShares = IntStream.range(0, LowMcUtils.ROUND)
            .mapToObj(roundIndex -> LowMcUtils.KEY_MATRICES[roundIndex + 1].lmul(senderShareKeyLongs))
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
        byte[] sbox0 = bcSender.and(
            BcSquareVector.create(baa0, LowMcUtils.SBOX_NUM * 3 * roundBatchSize, false),
            BcSquareVector.create(ccb0, LowMcUtils.SBOX_NUM * 3 * roundBatchSize, false)
        ).getBytes();
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
            BcSquareVector a0Sbox = bcSender.xor(
                BcSquareVector.create(a0, batchSize, false),
                BcSquareVector.create(bc0, batchSize, false)
            );
            byte[] a0SboxBytes = a0Sbox.getBytes();
            // b = a ⊕ b ⊕ (a ☉ c)
            byte[] ac0 = new byte[batchByteSize];
            System.arraycopy(sbox0, offset + batchByteSize, ac0, 0, batchByteSize);
            BytesUtils.reduceByteArray(ac0, batchSize);
            BcSquareVector b0Sbox = bcSender.xor(
                BcSquareVector.create(a0, batchSize, false),
                BcSquareVector.create(b0, batchSize, false)
            );
            b0Sbox = bcSender.xor(b0Sbox, BcSquareVector.create(ac0, batchSize, false));
            byte[] b0SboxBytes = b0Sbox.getBytes();
            // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
            byte[] ab0 = new byte[batchByteSize];
            System.arraycopy(sbox0, offset + 2 * batchByteSize, ab0, 0, batchByteSize);
            BytesUtils.reduceByteArray(ab0, batchSize);
            BcSquareVector c0Sbox = bcSender.xor(
                BcSquareVector.create(a0, batchSize, false),
                BcSquareVector.create(b0, batchSize, false)
            );
            c0Sbox = bcSender.xor(c0Sbox, BcSquareVector.create(c0, batchSize, false));
            c0Sbox = bcSender.xor(c0Sbox, BcSquareVector.create(ab0, batchSize, false));
            byte[] c0SboxBytes = c0Sbox.getBytes();
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
            .mapToObj(row -> LowMcUtils.LINEAR_MATRICES[roundIndex].lmul(states[row]))
            .toArray(long[][]::new);
    }

    private void addConstants(long[][] states, int roundIndex) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(states[row], LowMcUtils.CONSTANTS[roundIndex]));
    }

    private void addRoundKeys(long[][] states, int roundIndex) {
        IntStream.range(0, batchSize).forEach(row -> LongUtils.xori(states[row], roundKeyShares[roundIndex]));
    }
}
