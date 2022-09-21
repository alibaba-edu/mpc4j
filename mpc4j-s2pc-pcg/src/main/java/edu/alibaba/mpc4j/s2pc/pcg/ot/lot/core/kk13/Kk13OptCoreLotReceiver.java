package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.kk13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.AbstractCoreLotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KK13-核2^l选1-LOT优化协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kk13OptCoreLotReceiver extends AbstractCoreLotReceiver {
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
     * 布尔矩阵
     */
    private TransBitMatrix tMatrix;

    public Kk13OptCoreLotReceiver(Rpc receiverRpc, Party senderParty, Kk13OptCoreLotConfig config) {
        super(Kk13OptCoreLotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
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
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

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
            taskId, getPtoDesc().getPtoId(), Kk13OriCoreLotPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        LotReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // 初始化密码学原语
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        tMatrix = TransBitMatrixFactory.createInstance(envType, num, outputBitLength, parallel);
        TransBitMatrix codeMatrix = TransBitMatrixFactory.createInstance(envType, outputBitLength, num, parallel);
        // 生成编码，不需要并发操作
        IntStream.range(0, num).forEach(index ->
            codeMatrix.setColumn(index, linearCoder.encode(
                BytesUtils.paddingByteArray(choices[index], linearCoder.getDatawordByteLength())
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
                BytesUtils.reduceByteArray(tBytes, num);
                tMatrix.setColumn(columnIndex, tBytes);
                // and u^i = t^i ⊕ G(k_i^1) ⊕ r
                byte[] uBytes = prg.extendToBytes(crhf.hash(cotSenderOutput.getR1(columnIndex)));
                BytesUtils.reduceByteArray(uBytes, num);
                BytesUtils.xori(uBytes, tBytes);
                BytesUtils.xori(uBytes, codeTransposeMatrix.getColumn(columnIndex));

                return uBytes;
            })
            .collect(Collectors.toList());
    }

    private LotReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] qsArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return LotReceiverOutput.create(inputBitLength, outputBitLength, choices, qsArray);
    }
}
