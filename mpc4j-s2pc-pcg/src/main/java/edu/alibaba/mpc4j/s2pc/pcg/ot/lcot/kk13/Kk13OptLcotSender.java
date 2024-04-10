package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.AbstractLcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.Kk13OptLcotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * KK13-2^l选1-COT优化协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kk13OptLcotSender extends AbstractLcotSender {
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * KDF-OT协议接收方输出
     */
    private KdfOtReceiverOutput kdfOtReceiverOutput;

    public Kk13OptLcotSender(Rpc senderRpc, Party receiverParty, Kk13OptLcotConfig config) {
        super(Kk13OptLcotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int inputBitLength, byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, delta, maxNum);
        init();
    }

    @Override
    public byte[] init(int inputBitLength, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, maxNum);
        init();
        return BytesUtils.clone(delta);
    }

    private void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init(deltaBinary.length);
        kdfOtReceiverOutput = new KdfOtReceiverOutput(envType, coreCotReceiver.receive(deltaBinary));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LcotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId());
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        LcotSenderOutput senderOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyGenTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private LcotSenderOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == outputBitLength);
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(envType, num, outputBitLength, parallel);
        byte[][] uArray = matrixPayload.toArray(new byte[0][]);
        // 矩阵生成流
        IntStream qMatrixIntStream = IntStream.range(0, outputBitLength);
        qMatrixIntStream = parallel ? qMatrixIntStream.parallel() : qMatrixIntStream;
        qMatrixIntStream.forEach(columnIndex -> {
            byte[] columnBytes = prg.extendToBytes(kdfOtReceiverOutput.getKb(columnIndex, extraInfo));
            BytesUtils.reduceByteArray(columnBytes, num);
            if (deltaBinary[columnIndex]) {
                BytesUtils.xori(columnBytes, uArray[columnIndex]);
            }
            qMatrix.setColumn(columnIndex, columnBytes);
        });
        // 矩阵转置，方便按行获取Q
        TransBitMatrix qMatrixTranspose = qMatrix.transpose();
        // 生成qs
        byte[][] qsArray = IntStream.range(0, num)
            .mapToObj(qMatrixTranspose::getColumn)
            .toArray(byte[][]::new);
        return LcotSenderOutput.create(inputBitLength, delta, qsArray);
    }
}
