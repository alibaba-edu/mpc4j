package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.AbstractCoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotPtoDesc.PtoStep;

/**
 * IKNP03-核COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2020/05/27
 */
public class Iknp03CoreCotReceiver extends AbstractCoreCotReceiver {
    /**
     * 基础OT协议发送方
     */
    private final BaseOtSender baseOtSender;
    /**
     * KDF-OT协议输出
     */
    private KdfOtSenderOutput kdfOtSenderOutput;
    /**
     * 布尔矩阵
     */
    private TransBitMatrix tMatrix;

    public Iknp03CoreCotReceiver(Rpc receiverRpc, Party senderParty, Iknp03CoreCotConfig config) {
        super(Iknp03CoreCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtSender = BaseOtFactory.createSender(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtSender.init();
        kdfOtSenderOutput = new KdfOtSenderOutput(envType, baseOtSender.send(CommonConstants.BLOCK_BIT_LENGTH));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        CotReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyGenTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // 将选择比特组合成byte[]，方便在矩阵中执行xor运算
        byte[] choiceBytes = BinaryUtils.binaryToRoundByteArray(choices);
        // 初始化伪随机数生成器
        Prg prg = PrgFactory.createInstance(envType, choiceBytes.length);
        // 构建矩阵tMatrix
        tMatrix = TransBitMatrixFactory.createInstance(envType, num, CommonConstants.BLOCK_BIT_LENGTH, parallel);
        // 矩阵列加密流
        IntStream columnIndexIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
        return columnIndexIntStream
            .mapToObj(columnIndex -> {
                // Receiver forms m \times k matrices T_0, T_1 such that t_{j, 0} \oplus t_{j, 1} = (r_j || \cdots || r_j)
                byte[] columnSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(columnSeed);
                byte[] column0Bytes = prg.extendToBytes(columnSeed);
                BytesUtils.reduceByteArray(column0Bytes, num);
                tMatrix.setColumn(columnIndex, column0Bytes);
                byte[] column1Bytes = BytesUtils.xor(column0Bytes, choiceBytes);
                // Sender and receiver interact with OT^k_m: the receiver acts as OT sender with input t_0, t_1
                byte[] message0 = prg.extendToBytes(kdfOtSenderOutput.getK0(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(message0, num);
                BytesUtils.xori(message0, column0Bytes);
                byte[] message1 = prg.extendToBytes(kdfOtSenderOutput.getK1(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(message1, num);
                BytesUtils.xori(message1, column1Bytes);

                return new byte[][]{message0, message1};
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private CotReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return CotReceiverOutput.create(choices, rbArray);
    }
}
