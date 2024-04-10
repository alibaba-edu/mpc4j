package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.AbstractHammingParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * BCP13半诚实安全汉明距离协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public class Bcp13ShHammingReceiver extends AbstractHammingParty {
    /**
     * COT协议接收方
     */
    private final CotReceiver cotReceiver;

    public Bcp13ShHammingReceiver(Rpc receiverRpc, Party senderParty, Bcp13ShHammingConfig config) {
        super(Bcp13ShHammingPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
    }

    @Override
    public void init(int maxBitNum) throws MpcAbortException {
        setInitInput(maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT receiver
        cotReceiver.init(maxBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void sendHammingDistance(SquareZ2Vector x1) throws MpcAbortException {
        setPtoInput(x1);
        logPhaseInfo(PtoState.PTO_BEGIN, "Receiver sends hamming distance");

        int t = executeOtSteps(x1);
        stopWatch.start();
        List<byte[]> tPayload = new LinkedList<>();
        tPayload.add(IntUtils.boundedNonNegIntToByteArray(t, bitNum));
        DataPacketHeader tHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_T.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(tHeader,tPayload));
        stopWatch.stop();
        long tTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, tTime);

        logPhaseInfo(PtoState.PTO_END, "Receiver sends hamming distance");
    }

    @Override
    public int receiveHammingDistance(SquareZ2Vector x1) throws MpcAbortException {
        setPtoInput(x1);
        logPhaseInfo(PtoState.PTO_BEGIN, "Receiver receives hamming distance");

        int t = executeOtSteps(x1);
        stopWatch.start();
        DataPacketHeader rHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_R.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> rPayload = rpc.receive(rHeader).getPayload();
        MpcAbortPreconditions.checkArgument(rPayload.size() == 1);
        int r = IntUtils.byteArrayToBoundedNonNegInt(rPayload.remove(0), bitNum);
        int hammingDistance = (t - r) % (bitNum + 1);
        hammingDistance = hammingDistance < 0 ? hammingDistance + bitNum + 1 : hammingDistance;
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, rTime);

        logPhaseInfo(PtoState.PTO_END, "Receiver receives hamming distance");
        return hammingDistance;
    }

    private int executeOtSteps(SquareZ2Vector x0) throws MpcAbortException {
        stopWatch.start();
        boolean[] ys = BinaryUtils.byteArrayToBinary(x0.getBitVector().getBytes(), bitNum);
        stopWatch.stop();
        long ysTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, ysTime);

        stopWatch.start();
        // P_1 and P_2 engage in a OT_1^2, where P_2's selection bit is y_i.
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(ys);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOAD.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderMessagePayload = rpc.receive(senderMessageHeader).getPayload();
        MpcAbortPreconditions.checkArgument(senderMessagePayload.size() == bitNum * 2);
        byte[][] senderMessageFlattenArray = senderMessagePayload.toArray(new byte[0][]);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(bitNum);
        int[] ts = IntStream.range(0, bitNum)
            .map(index -> {
                byte[] keyi = Arrays.copyOf(rotReceiverOutput.getRb(index), messageByteLength);
                byte[] choiceCiphertext = ys[index] ?
                    senderMessageFlattenArray[index * 2 + 1] : senderMessageFlattenArray[index * 2];
                BytesUtils.xori(choiceCiphertext, keyi);
                return IntUtils.byteArrayToBoundedNonNegInt(choiceCiphertext, bitNum);
            })
            .toArray();
        int t = 0;
        for (int index = 0; index < bitNum; index++) {
            t = (t + ts[index]) % (bitNum + 1);
        }
        t = t < 0 ? t + bitNum + 1 : t;
        stopWatch.stop();
        long otTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, otTime);

        return t;
    }
}
