package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotPtoDesc.PtoStep;

/**
 * ALSZ13-核COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2020/06/02
 */
public class Alsz13CoreCotReceiver extends AbstractCoreCotReceiver {
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

    public Alsz13CoreCotReceiver(Rpc receiverRpc, Party senderParty, Alsz13CoreCotConfig config) {
        super(Alsz13CoreCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        byte[] rBytes = BinaryUtils.binaryToRoundByteArray(choices);
        // 初始化伪随机数生成器
        Prg prg = PrgFactory.createInstance(envType, rBytes.length);
        // 构建矩阵tMatrix
        tMatrix = TransBitMatrixFactory.createInstance(envType, num, CommonConstants.BLOCK_BIT_LENGTH, parallel);
        // 用密钥扩展得到矩阵T
        IntStream columnIndexIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
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
                BytesUtils.xori(uBytes, rBytes);

                return uBytes;
            })
            .collect(Collectors.toList());
    }

    private CotReceiverOutput generateReceiverOutput() {
        // 将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);
        // 打包u^i
        return CotReceiverOutput.create(choices, rbArray);
    }
}
