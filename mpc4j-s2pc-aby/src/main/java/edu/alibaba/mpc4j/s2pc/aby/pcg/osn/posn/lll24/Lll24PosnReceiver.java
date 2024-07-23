package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.AbstractPosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24.Lll24PosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24 Pre-computed OSN receiver.
 *
 * @author Feng Han
 * @date 2024/5/08
 */
public class Lll24PosnReceiver extends AbstractPosnReceiver {

    public Lll24PosnReceiver(Rpc receiverRpc, Party senderParty, Lll24PosnConfig config) {
        super(Lll24PosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
    }

    @Override
    public DosnPartyOutput posn(int[] pi, int byteLength, RosnReceiverOutput preRosnReceiverOutput) throws MpcAbortException {
        setPtoInput(pi, byteLength, preRosnReceiverOutput);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int[] invSigma = new int[num];
        int[] sigma = preRosnReceiverOutput.getPi();
        IntStream.range(0, num).forEach(i -> invSigma[sigma[i]] = i);
        int[] piInvSigma = PermutationNetworkUtils.permutation(pi, invSigma);
        byte[] data = IntUtils.intArrayToByteArray(piInvSigma);
        DataPacketHeader permutationDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PERMUTATION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(permutationDataPacketHeader, Collections.singletonList(data)));
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sendTime);

        stopWatch.start();
        byte[][] processPre = PermutationNetworkUtils.permutation(piInvSigma, preRosnReceiverOutput.getDeltas());

        DataPacketHeader maskInputDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MASK.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> maskInputDataPacketPayload = rpc.receive(maskInputDataPacketHeader).getPayload();

        byte[][] mask = PermutationNetworkUtils.permutation(pi, maskInputDataPacketPayload.toArray(new byte[0][]));
        for (int i = 0; i < num; i++) {
            BytesUtils.xori(processPre[i], mask[i]);
        }
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, receiveTime);

        logPhaseInfo(PtoState.PTO_END);
        return new DosnPartyOutput(processPre);
    }
}