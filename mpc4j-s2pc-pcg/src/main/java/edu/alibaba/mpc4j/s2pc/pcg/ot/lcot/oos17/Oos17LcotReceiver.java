package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.AbstractLcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OOS17-2^l选1-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/8
 */
public class Oos17LcotReceiver extends AbstractLcotReceiver {
    /**
     * COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * KDF-OT协议发送方输出
     */
    private KdfOtSenderOutput kdfOtSenderOutput;
    /**
     * 随机预言机密钥
     */
    private byte[] randomOracleKey;
    /**
     * 扩展数量
     */
    private int extendNum;
    /**
     * 扩展选择数组
     */
    private byte[][] extendChoices;
    /**
     * 矩阵T
     */
    private TransBitMatrix tMatrix;
    /**
     * 转置矩阵T
     */
    private TransBitMatrix tTransposeMatrix;

    public Oos17LcotReceiver(Rpc receiverRpc, Party senderParty, Oos17LcotConfig config) {
        super(Oos17LcotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public int init(int inputBitLength, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] cotDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(cotDelta);
        coreCotSender.init(cotDelta, outputBitLength);
        kdfOtSenderOutput = new KdfOtSenderOutput(envType, coreCotSender.send(outputBitLength));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Oos17LcotPtoDesc.PtoStep.SENDER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomOracleKeyPayload = rpc.receive(randomOracleKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(randomOracleKeyPayload.size() == 1);
        randomOracleKey = randomOracleKeyPayload.remove(0);
        stopWatch.stop();
        long randomOracleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, randomOracleTime);

        logPhaseInfo(PtoState.INIT_END);
        return linearCoder.getCodewordBitLength();
    }

    @Override
    public LcotReceiverOutput receive(byte[][] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Oos17LcotPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, keyGenTime);

        stopWatch.start();
        List<byte[]> correlateCheckPayload = generateCorrelateCheckPayload();
        DataPacketHeader correlateCheckHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Oos17LcotPtoDesc.PtoStep.RECEIVER_SEND_CHECK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateCheckHeader, correlateCheckPayload));
        LcotReceiverOutput receiverOutput = generateReceiverOutput();
        tTransposeMatrix = null;
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, checkTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // Let m' = m + s
        extendNum = num + CommonConstants.STATS_BIT_LENGTH;
        int extendByteNum = CommonUtils.getByteLength(extendNum);
        extendChoices = new byte[extendNum][];
        // x_i = x_i for i ∈ m
        System.arraycopy(choices, 0, extendChoices, 0, num);
        // x_i is random for i ∈ [m + 1, m']
        for (int extendIndex = num; extendIndex < extendNum; extendIndex++) {
            extendChoices[extendIndex] = new byte[inputByteLength];
            secureRandom.nextBytes(extendChoices[extendIndex]);
            BytesUtils.reduceByteArray(extendChoices[extendIndex], inputBitLength);
        }
        // 初始化密码学原语
        Prg prg = PrgFactory.createInstance(envType, extendByteNum);
        tMatrix = TransBitMatrixFactory.createInstance(envType, extendNum, outputBitLength, parallel);
        TransBitMatrix codeMatrix = TransBitMatrixFactory.createInstance(envType, outputBitLength, extendNum, parallel);
        // 生成编码，不需要并发操作
        IntStream.range(0, extendNum).forEach(l ->
            codeMatrix.setColumn(l, linearCoder.encode(
                BytesUtils.paddingByteArray(extendChoices[l], linearCoder.getDatawordByteLength())
            ))
        );
        // 将此编码转置
        TransBitMatrix codeTransposeMatrix = codeMatrix.transpose();
        // 用密钥扩展得到矩阵T
        IntStream columnIndexIntStream = IntStream.range(0, outputBitLength);
        columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
        return columnIndexIntStream
            .mapToObj(columnIndex -> {
                // R computes t^i = G(k^0_i)
                byte[] tBytes = prg.extendToBytes(kdfOtSenderOutput.getK0(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(tBytes, extendNum);
                tMatrix.setColumn(columnIndex, tBytes);
                // and u^i = t^i ⊕ G(k_i^1) ⊕ r
                byte[] uBytes = prg.extendToBytes(kdfOtSenderOutput.getK1(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(uBytes, extendNum);
                BytesUtils.xori(uBytes, tBytes);
                BytesUtils.xori(uBytes, codeTransposeMatrix.getColumn(columnIndex));

                return uBytes;
            })
            .collect(Collectors.toList());
    }

    private List<byte[]> generateCorrelateCheckPayload() {
        // 设置随机预言机
        Prf randomOracle = PrfFactory.createInstance(envType, byteNum);
        randomOracle.setKey(randomOracleKey);
        // 矩阵转置，得到t
        tTransposeMatrix = tMatrix.transpose();
        tMatrix = null;
        IntStream correlateCheckIntStream = IntStream.range(0, CommonConstants.STATS_BIT_LENGTH);
        correlateCheckIntStream = parallel ? correlateCheckIntStream.parallel() : correlateCheckIntStream;
        return correlateCheckIntStream
            .mapToObj(l -> {
                byte[][] tw = new byte[2][];
                // 调用随机预言的输入是extraInfo || l
                byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                    .putLong(extraInfo).putInt(l).array();
                // R samples random string (x_1^(l), ..., x_m^(l)) ∈ F_2^m
                byte[] xl = randomOracle.getBytes(indexMessage);
                BytesUtils.reduceByteArray(xl, num);
                boolean[] xlBinary = BinaryUtils.byteArrayToBinary(xl, num);
                // t^(l) = Σ_{i ∈ [m]} t_i·x_i^(l) + t_{m + l}
                tw[0] = new byte[outputByteLength];
                for (int i = 0; i < num; i++) {
                    if (xlBinary[i]) {
                        BytesUtils.xori(tw[0], tTransposeMatrix.getColumn(i));
                    }
                }
                BytesUtils.xori(tw[0], tTransposeMatrix.getColumn(num + l));
                // w^(l) = Σ_{i ∈ [m]} w_i·x_i^(l) + w_{m + l}
                tw[1] = new byte[inputByteLength];
                for (int i = 0; i < num; i++) {
                    if (xlBinary[i]) {
                        BytesUtils.xori(tw[1], extendChoices[i]);
                    }
                }
                BytesUtils.xori(tw[1], extendChoices[num + l]);
                return tw;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private LcotReceiverOutput generateReceiverOutput() {
        byte[][] qsArray = IntStream.range(0, num)
            .mapToObj(tTransposeMatrix::getColumn)
            .toArray(byte[][]::new);
        return LcotReceiverOutput.create(inputBitLength, outputBitLength, choices, qsArray);
    }
}
