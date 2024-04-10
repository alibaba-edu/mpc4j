package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.AbstractCoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KOS15-核COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kos15CoreCotSender extends AbstractCoreCotSender {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * GF(2^64)运算接口
     */
    private final Gf64 gf64;
    /**
     * KDF-OT协议输出
     */
    private KdfOtReceiverOutput kdfOtReceiverOutput;
    /**
     * 扩展数量
     */
    private int extendNum;
    /**
     * 扩展字节数量
     */
    private int extendByteNum;
    /**
     * Q的转置矩阵
     */
    private TransBitMatrix qTransposeMatrix;
    /**
     * Q的分块元素
     */
    private byte[][][] qBlock;
    /**
     * partition num
     */
    private int m;
    /**
     * bit length
     */
    private final int s;

    public Kos15CoreCotSender(Rpc senderRpc, Party receiverParty, Kos15CoreCotConfig config) {
        super(Kos15CoreCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
        gf64 = Gf64Factory.createInstance(envType, config.getGf64Type());
        s = gf64.getL();
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
        // l' = l + s
        m = CommonUtils.getUnitNum(num, s);
        extendNum = (m + 1) * s;
        extendByteNum = CommonUtils.getByteLength(extendNum);
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, matrixTime);

        stopWatch.start();
        List<byte[]> chiPolynomialPayload = sampleChiPolynomial();
        DataPacketHeader chiPolynomialHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_POLYNOMIAL.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(chiPolynomialHeader, chiPolynomialPayload));
        stopWatch.stop();
        long sampleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, sampleTime);

        // consistency check
        stopWatch.start();
        DataPacketHeader correlateCheckHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlateCheckPayload = rpc.receive(correlateCheckHeader).getPayload();
        CotSenderOutput senderOutput = handleCorrelateCheckPayload(correlateCheckPayload, chiPolynomialPayload);
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, checkTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == CommonConstants.BLOCK_BIT_LENGTH);
        Prg prg = PrgFactory.createInstance(envType, extendByteNum);
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(
            envType, extendNum, CommonConstants.BLOCK_BIT_LENGTH, parallel
        );
        // 设置矩阵Q的每一列
        byte[][] uArray = matrixPayload.toArray(new byte[0][]);
        qBlock = new byte[CommonConstants.BLOCK_BIT_LENGTH][m + 1][CommonUtils.getByteLength(s)];
        IntStream qMatrixIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        qMatrixIntStream = parallel ? qMatrixIntStream.parallel() : qMatrixIntStream;
        qMatrixIntStream.forEach(columnIndex -> {
            byte[] columnBytes = prg.extendToBytes(kdfOtReceiverOutput.getKb(columnIndex, extraInfo));
            BytesUtils.reduceByteArray(columnBytes, extendNum);
            if (deltaBinary[columnIndex]) {
                BytesUtils.xori(columnBytes, uArray[columnIndex]);
            }
            qMatrix.setColumn(columnIndex, columnBytes);
            ByteBuffer flattenColumn = ByteBuffer.wrap(columnBytes);
            for (int j = 0; j < m + 1; j++) {
                flattenColumn.get(qBlock[columnIndex][j]);
            }
        });
        // 矩阵转置，方便按行获取Q
        qTransposeMatrix = qMatrix.transpose();
    }

    private List<byte[]> sampleChiPolynomial() {
        return IntStream.range(0, m)
            .mapToObj(i -> gf64.createRandom(secureRandom))
            .collect(Collectors.toList());
    }

    private CotSenderOutput handleCorrelateCheckPayload(List<byte[]> correlateCheckPayload,
                                                        List<byte[]> chiPolynomial) throws MpcAbortException {
        // 包含x和t_i, i = 1,...,k
        MpcAbortPreconditions.checkArgument(correlateCheckPayload.size() == CommonConstants.BLOCK_BIT_LENGTH + 1);
        // 解包x和t
        byte[] xPolynomial = correlateCheckPayload.remove(0);
        byte[][] tPolynomial = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH)
            .mapToObj(correlateCheckPayload::get)
            .toArray(byte[][]::new);
        IntStream intStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(columnIndex -> {
            byte[] qi = gf64.createZero();
            for (int j = 0; j < m; j++) {
                gf64.muli(qBlock[columnIndex][j], chiPolynomial.get(j));
                gf64.addi(qi, qBlock[columnIndex][j]);
            }
            gf64.addi(qi, qBlock[columnIndex][m]);
            if (deltaBinary[columnIndex]) {
                gf64.subi(qi, xPolynomial);
            }
            try {
                MpcAbortPreconditions.checkArgument(Arrays.equals(qi, tPolynomial[columnIndex]));
            } catch (MpcAbortException e) {
                e.printStackTrace();
            }
        });
        // 生成r0
        byte[][] r0Array = IntStream.range(0, num)
            .mapToObj(qTransposeMatrix::getColumn)
            .toArray(byte[][]::new);
        return CotSenderOutput.create(delta, r0Array);
    }
}
