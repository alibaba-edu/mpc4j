package edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoder;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoderUtils;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.AbstractOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSenderOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * KKRT16-ORI-OPRF协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/05
 */
public class Kkrt16OriOprfSender extends AbstractOprfSender {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
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
     * 矩阵Q的密钥
     */
    private byte[][] qMatrixKey;

    public Kkrt16OriOprfSender(Rpc senderRpc, Party receiverParty, Kkrt16OriOprfConfig config) {
        super(Kkrt16OriOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
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
    public void init(int maxBatchSize) throws MpcAbortException {
        setBatchInitInput(maxBatchSize);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化码字字节长度
        codewordByteLength = RandomCoderUtils.getCodewordByteLength(maxBatchSize);
        codewordBitLength = codewordByteLength * Byte.SIZE;
        // 初始化COT协议
        coreCotReceiver.init(codewordBitLength);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initCotTime);

        stopWatch.start();
        // 生成关联值Δ
        delta = new byte[codewordByteLength];
        secureRandom.nextBytes(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta);
        // 执行COT
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(deltaBinary);
        // 将COT转换为密钥
        IntStream qMatrixKeyIntStream = IntStream.range(0, codewordBitLength);
        qMatrixKeyIntStream = parallel ? qMatrixKeyIntStream.parallel() : qMatrixKeyIntStream;
        qMatrixKey = qMatrixKeyIntStream
            .mapToObj(cotReceiverOutput::getRb)
            .map(crhf::hash)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public OprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setBatchPtoInput(batchSize);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化伪随机编码
        DataPacketHeader keyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Kkrt16OriOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keyPayload = rpc.receive(keyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(keyPayload.size() == 1);
        randomCoderKey = keyPayload.remove(0);
        stopWatch.stop();
        long initKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initKeyTime);

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Kkrt16OriOprfPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        OprfSenderOutput senderOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), matrixTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private OprfSenderOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == 2 * codewordBitLength);
        Prg prg = PrgFactory.createInstance(envType, CommonUtils.getByteLength(batchSize));
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(envType, batchSize, codewordBitLength, parallel);
        byte[][] tMatrixFlattenedCiphertext = matrixPayload.toArray(new byte[0][]);
        // 矩阵生成流
        IntStream matrixColumnIntStream = IntStream.range(0, codewordBitLength);
        matrixColumnIntStream = parallel ? matrixColumnIntStream.parallel() : matrixColumnIntStream;
        matrixColumnIntStream.forEach(columnIndex -> {
            byte[] columnBytes = prg.extendToBytes(qMatrixKey[columnIndex]);
            BytesUtils.reduceByteArray(columnBytes, batchSize);
            byte[] message = deltaBinary[columnIndex] ?
                tMatrixFlattenedCiphertext[2 * columnIndex + 1] : tMatrixFlattenedCiphertext[2 * columnIndex];
            BytesUtils.xori(columnBytes, message);
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
