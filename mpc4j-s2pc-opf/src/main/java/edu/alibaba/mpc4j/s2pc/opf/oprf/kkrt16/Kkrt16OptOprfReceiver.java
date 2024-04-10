package edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoder;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoderUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KKRT16-OPT-OPRF协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class Kkrt16OptOprfReceiver extends AbstractOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * 码字字节长度
     */
    private int codewordByteLength;
    /**
     * 码字比特长度
     */
    private int codewordBitLength;
    /**
     * KDF-OT输出
     */
    private KdfOtSenderOutput kdfOtSenderOutput;
    /**
     * 伪随机编码
     */
    private byte[] randomCoderKey;
    /**
     * 布尔矩阵
     */
    private TransBitMatrix tMatrix;

    public Kkrt16OptOprfReceiver(Rpc receiverRpc, Party senderParty, Kkrt16OptOprfConfig config) {
        super(Kkrt16OptOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 设置伪随机编码码字比特长度
        codewordByteLength = RandomCoderUtils.getCodewordByteLength(Math.max(maxBatchSize, maxPrfNum));
        codewordBitLength = codewordByteLength * Byte.SIZE;
        byte[] cotDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(cotDelta);
        // 初始化COT协议
        coreCotSender.init(cotDelta, codewordBitLength);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initCotTime);

        stopWatch.start();
        kdfOtSenderOutput = new KdfOtSenderOutput(envType, coreCotSender.send(codewordBitLength));
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, cotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 生成伪随机编码密钥
        randomCoderKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomCoderKey);
        List<byte[]> keyPayload = new LinkedList<>();
        keyPayload.add(randomCoderKey);
        // 发送伪随机编码密钥
        DataPacketHeader keyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kkrt16OptOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keyHeader, keyPayload));
        stopWatch.stop();
        long initKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, initKeyTime, "Receiver sends PRC key");

        stopWatch.start();
        // 生成矩阵
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kkrt16OptOprfPtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, matrixTime, "Receiver generates matrix");

        stopWatch.start();
        OprfReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, keyGenTime, "Receiver generates OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // 初始化伪随机数生成器
        Prg prg = PrgFactory.createInstance(envType, CommonUtils.getByteLength(batchSize));
        // 设置随机编码矩阵
        RandomCoder randomCoder = new RandomCoder(envType, codewordByteLength);
        randomCoder.setKey(randomCoderKey);
        TransBitMatrix prcMatrix = TransBitMatrixFactory.createInstance(envType, codewordBitLength, batchSize, parallel);
        IntStream choicesIntStream = IntStream.range(0, batchSize);
        choicesIntStream = parallel ? choicesIntStream.parallel() : choicesIntStream;
        choicesIntStream.forEach(index -> {
            byte[] encode = randomCoder.encode(inputs[index]);
            prcMatrix.setColumn(index, encode);
        });
        randomCoderKey = null;
        // 转置随机编码矩阵
        TransBitMatrix prcTransposeMatrix = prcMatrix.transpose();
        // 创建矩阵T0
        tMatrix = TransBitMatrixFactory.createInstance(envType, batchSize, codewordBitLength, parallel);
        IntStream tMatrixIntStream = IntStream.range(0, codewordBitLength);
        tMatrixIntStream = parallel ? tMatrixIntStream.parallel() : tMatrixIntStream;
        return tMatrixIntStream.mapToObj(columnIndex -> {
            // 构建矩阵U = T_0 + T_1 + X，其中X为扩展选择比特向量
            byte[] t0Bytes = prg.extendToBytes(kdfOtSenderOutput.getK0(columnIndex, extraInfo));
            BytesUtils.reduceByteArray(t0Bytes, batchSize);
            tMatrix.setColumn(columnIndex, t0Bytes);
            byte[] uBytes = prg.extendToBytes(kdfOtSenderOutput.getK1(columnIndex, extraInfo));
            BytesUtils.reduceByteArray(uBytes, batchSize);
            BytesUtils.xori(uBytes, t0Bytes);
            BytesUtils.xori(uBytes, prcTransposeMatrix.getColumn(columnIndex));
            return uBytes;
        }).collect(Collectors.toList());
    }

    private OprfReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] ts = IntStream.range(0, batchSize)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);
        // 接收方输出只需要读取randomCoder的长度信息，因此可以直接传入
        return new OprfReceiverOutput(codewordByteLength, inputs, ts);
    }
}
