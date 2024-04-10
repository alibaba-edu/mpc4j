package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.AbstractCoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * ALSZ13-核COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2020/06/02
 */
public class Alsz13CoreCotSender extends AbstractCoreCotSender {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * KDF-OT协议输出
     */
    private KdfOtReceiverOutput kdfOtReceiverOutput;

    public Alsz13CoreCotSender(Rpc senderRpc, Party receiverParty, Alsz13CoreCotConfig config) {
        super(Alsz13CoreCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
    }

    @Override
    public void init(byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtReceiver.init();
        kdfOtReceiverOutput = new KdfOtReceiverOutput(envType, baseOtReceiver.receive(deltaBinary));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        CotSenderOutput senderOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private CotSenderOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == CommonConstants.BLOCK_BIT_LENGTH);
        Prg prg = PrgFactory.createInstance(envType, CommonUtils.getByteLength(num));
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(envType, num, CommonConstants.BLOCK_BIT_LENGTH, parallel);
        // 设置矩阵Q的每一列
        byte[][] uArray = matrixPayload.toArray(new byte[0][]);
        IntStream qMatrixIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
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
        // 生成r0
        byte[][] r0Array = IntStream.range(0, num)
            .mapToObj(qMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return CotSenderOutput.create(delta, r0Array);
    }
}