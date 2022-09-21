package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.kk13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.AbstractCoreLotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * KK13-核2^l选1-OT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public class Kk13OriCoreLotSender extends AbstractCoreLotSender {
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
    /**
     * COT协议接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;

    public Kk13OriCoreLotSender(Rpc senderRpc, Party receiverParty, Kk13OriCoreLotConfig config) {
        super(Kk13OriCoreLotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreCotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, delta, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreCotReceiver.init(outputBitLength);
        cotReceiverOutput = coreCotReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void init(int inputBitLength, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreCotReceiver.init(deltaBinary.length);
        cotReceiverOutput = coreCotReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CoreLotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, ptoDesc.getPtoId(), Kk13OriCoreLotPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId());
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        CoreLotSenderOutput senderOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private CoreLotSenderOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == 2 * outputBitLength);
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(envType, num, outputBitLength, parallel);
        byte[][] tMatrixFlattenedCiphertext = matrixPayload.toArray(new byte[0][]);
        // 矩阵生成流
        IntStream matrixColumnIntStream = IntStream.range(0, outputBitLength);
        matrixColumnIntStream = parallel ? matrixColumnIntStream.parallel() : matrixColumnIntStream;
        matrixColumnIntStream.forEach(columnIndex -> {
            byte[] columnBytes = prg.extendToBytes(crhf.hash(cotReceiverOutput.getRb(columnIndex)));
            BytesUtils.reduceByteArray(columnBytes, num);
            byte[] message = cotReceiverOutput.getChoice(columnIndex) ?
                tMatrixFlattenedCiphertext[2 * columnIndex + 1] : tMatrixFlattenedCiphertext[2 * columnIndex];
            BytesUtils.xori(columnBytes, message);
            qMatrix.setColumn(columnIndex, columnBytes);
        });
        // 矩阵转置，方便按行获取Q
        TransBitMatrix qMatrixTranspose = qMatrix.transpose();
        byte[][] qsArray = IntStream.range(0, num)
            .mapToObj(qMatrixTranspose::getColumn)
            .toArray(byte[][]::new);
        return CoreLotSenderOutput.create(inputBitLength, delta, qsArray);
    }
}
