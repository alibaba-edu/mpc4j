package edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoder;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoderUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * KKRT16-OPT-OPRF协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class Kkrt16OptOprfSender extends AbstractOprfSender {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 关联值Δ
     */
    private byte[] delta;
    /**
     * 关联值比特
     */
    private boolean[] deltaBinary;
    /**
     * 随机编码密钥
     */
    private byte[] randomCoderKey;
    /**
     * 码字字节长度
     */
    private int codewordByteLength;
    /**
     * 码字比特长度
     */
    private int codewordBitLength;
    /**
     * KDF-OT接收方输出
     */
    private KdfOtReceiverOutput kdfOtReceiverOutput;

    public Kkrt16OptOprfSender(Rpc senderRpc, Party receiverParty, Kkrt16OptOprfConfig config) {
        super(Kkrt16OptOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化码字字节长度
        codewordByteLength = RandomCoderUtils.getCodewordByteLength(Math.max(maxBatchSize, maxPrfNum));
        codewordBitLength = codewordByteLength * Byte.SIZE;
        // 初始化COT协议
        coreCotReceiver.init(codewordBitLength);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initCotTime);

        stopWatch.start();
        // 生成关联值Δ
        delta = new byte[codewordByteLength];
        secureRandom.nextBytes(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta);
        // 执行COT协议
        kdfOtReceiverOutput = new KdfOtReceiverOutput(envType, coreCotReceiver.receive(deltaBinary));
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, cotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 初始化伪随机编码
        DataPacketHeader keyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kkrt16OptOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keyPayload = rpc.receive(keyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(keyPayload.size() == 1);
        stopWatch.start();
        randomCoderKey = keyPayload.remove(0);
        stopWatch.stop();
        long initKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initKeyTime, "Sender receives PRC key");

        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kkrt16OptOprfPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        stopWatch.start();
        OprfSenderOutput senderOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, matrixTime, "Receiver generates OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private OprfSenderOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == codewordBitLength);
        Prg prg = PrgFactory.createInstance(envType, CommonUtils.getByteLength(batchSize));
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(envType, batchSize, codewordBitLength, parallel);
        // 设置矩阵U
        byte[][] uByteArrays = matrixPayload.toArray(new byte[0][]);
        // 矩阵生成流
        IntStream matrixColumnIntStream = IntStream.range(0, codewordBitLength);
        matrixColumnIntStream = parallel ? matrixColumnIntStream.parallel() : matrixColumnIntStream;
        matrixColumnIntStream.forEach(columnIndex -> {
            byte[] columnBytes = prg.extendToBytes(kdfOtReceiverOutput.getKb(columnIndex, extraInfo));
            BytesUtils.reduceByteArray(columnBytes, batchSize);
            if (deltaBinary[columnIndex]) {
                BytesUtils.xori(columnBytes, uByteArrays[columnIndex]);
            }
            qMatrix.setColumn(columnIndex, columnBytes);
        });
        // 矩阵转置，方便按行获取Q
        TransBitMatrix qMatrixTranspose = qMatrix.transpose();
        byte[][] r0Array = IntStream.range(0, batchSize)
            .mapToObj(qMatrixTranspose::getColumn)
            .toArray(byte[][]::new);
        // 创建发送方输出
        RandomCoder randomCoder = new RandomCoder(envType, codewordByteLength);
        randomCoder.setKey(randomCoderKey);
        return new Kkrt16OprfSenderOutput(randomCoder, delta, r0Array);
    }
}
