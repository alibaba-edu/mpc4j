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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KK13-2^l选1-COT优化协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kk13OptLcotReceiver extends AbstractLcotReceiver {
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

    public Kk13OptLcotReceiver(Rpc receiverRpc, Party senderParty, Kk13OptLcotConfig config) {
        super(Kk13OptLcotPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        // 用密钥扩展得到矩阵T
        IntStream columnIndexIntStream = IntStream.range(0, outputBitLength);
        columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
        return columnIndexIntStream
            .mapToObj(columnIndex -> {
                // R computes t^i = G(k^0_i)
                byte[] tBytes = prg.extendToBytes(kdfOtSenderOutput.getK0(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(tBytes, num);
                tMatrix.setColumn(columnIndex, tBytes);
                // and u^i = t^i ⊕ G(k_i^1) ⊕ r
                byte[] uBytes = prg.extendToBytes(kdfOtSenderOutput.getK1(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(uBytes, num);
                BytesUtils.xori(uBytes, tBytes);
                BytesUtils.xori(uBytes, codeTransposeMatrix.getColumn(columnIndex));

                return uBytes;
            })
            .collect(Collectors.toList());
    }

    private LcotReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] qsArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return LcotReceiverOutput.create(inputBitLength, outputBitLength, choices, qsArray);
    }
}
