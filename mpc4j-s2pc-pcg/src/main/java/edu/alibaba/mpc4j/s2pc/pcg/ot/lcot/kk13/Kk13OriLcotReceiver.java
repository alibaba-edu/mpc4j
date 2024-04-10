package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.AbstractLcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.Kk13OriLcotPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KK13-2^l选1-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
public class Kk13OriLcotReceiver extends AbstractLcotReceiver {
    /**
     * COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * KDF-OT协议发送方输出
     */
    private KdfOtSenderOutput kdfOtSenderOutput;
    /**
     * 布尔矩阵
     */
    private TransBitMatrix tMatrix;

    public Kk13OriLcotReceiver(Rpc receiverRpc, Party senderParty, Kk13OriLcotConfig config) {
        super(Kk13OriLcotPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        LcotReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyGenTime);

        logPhaseInfo(PtoState.PTO_END);
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
        // 各个列加密
        IntStream tMatrixIntStream = IntStream.range(0, outputBitLength);
        tMatrixIntStream = parallel ? tMatrixIntStream.parallel() : tMatrixIntStream;
        return tMatrixIntStream
            .mapToObj(columnIndex -> {
                // The receiver forms m \times k matrices T_0, T_1 such that t_{j, 0} \oplus t_{j, 1} = C(r_j)
                byte[] columnSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(columnSeed);
                byte[] column0Bytes = prg.extendToBytes(columnSeed);
                BytesUtils.reduceByteArray(column0Bytes, num);
                tMatrix.setColumn(columnIndex, column0Bytes);
                byte[] column1Bytes = BytesUtils.xor(column0Bytes, codeTransposeMatrix.getColumn(columnIndex));
                byte[] message0 = prg.extendToBytes(kdfOtSenderOutput.getK0(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(message0, num);
                BytesUtils.xori(message0, column0Bytes);
                byte[] message1 = prg.extendToBytes(kdfOtSenderOutput.getK1(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(message1, num);
                BytesUtils.xori(message1, column1Bytes);
                // The sender and the receiver interact with OT^k_m: the receiver acts as OT sender with input t_0, t_1
                return new byte[][]{message0, message1};
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private LcotReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return LcotReceiverOutput.create(inputBitLength, outputBitLength, choices, rbArray);
    }
}
