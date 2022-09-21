package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.oos17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.AbstractCoreLotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OOS17-核2^l选1-OT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/8
 */
public class Oos17CoreLotReceiver extends AbstractCoreLotReceiver {
    /**
     * COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
    /**
     * COT协议发送方输出
     */
    private CotSenderOutput cotSenderOutput;
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

    public Oos17CoreLotReceiver(Rpc receiverRpc, Party senderParty, Oos17CoreLotConfig config) {
        super(Oos17CoreLotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO_SIGMA);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreCotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotSender.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[] cotDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(cotDelta);
        coreCotSender.init(cotDelta, outputBitLength);
        cotSenderOutput = coreCotSender.send(outputBitLength);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Oos17CoreLotPtoDesc.PtoStep.SENDER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomOracleKeyPayload = rpc.receive(randomOracleKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(randomOracleKeyPayload.size() == 1);
        randomOracleKey = randomOracleKeyPayload.remove(0);
        stopWatch.stop();
        long randomOracleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), randomOracleTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public LotReceiverOutput receive(byte[][] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Oos17CoreLotPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        stopWatch.start();
        List<byte[]> correlateCheckPayload = generateCorrelateCheckPayload();
        DataPacketHeader correlateCheckHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Oos17CoreLotPtoDesc.PtoStep.RECEIVER_SEND_CHECK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateCheckHeader, correlateCheckPayload));
        LotReceiverOutput receiverOutput = generateReceiverOutput();
        tTransposeMatrix = null;
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
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
                byte[] tBytes = prg.extendToBytes(crhf.hash(cotSenderOutput.getR0(columnIndex)));
                BytesUtils.reduceByteArray(tBytes, extendNum);
                tMatrix.setColumn(columnIndex, tBytes);
                // and u^i = t^i ⊕ G(k_i^1) ⊕ r
                byte[] uBytes = prg.extendToBytes(crhf.hash(cotSenderOutput.getR1(columnIndex)));
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

    private LotReceiverOutput generateReceiverOutput() {
        byte[][] qsArray = IntStream.range(0, num)
            .mapToObj(tTransposeMatrix::getColumn)
            .toArray(byte[][]::new);
        return LotReceiverOutput.create(inputBitLength, outputBitLength, choices, qsArray);
    }
}
