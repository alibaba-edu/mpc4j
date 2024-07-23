package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.AbstractPosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24.Lll24PosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LLL24 Pre-computed OSN sender.
 *
 * @author Feng Han
 * @date 2024/5/08
 */
public class Lll24PosnSender extends AbstractPosnSender {

    public Lll24PosnSender(Rpc senderRpc, Party receiverParty, Lll24PosnConfig config) {
        super(Lll24PosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
    }

    @Override
    public DosnPartyOutput posn(byte[][] inputVector, RosnSenderOutput preRosnSenderOutput) throws MpcAbortException {
        setPtoInput(inputVector, preRosnSenderOutput);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] preInput = preRosnSenderOutput.getAs();
        IntStream.range(0, num).forEach(i -> BytesUtils.xori(preInput[i], inputVector[i]));
        DataPacketHeader maskInputDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MASK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(maskInputDataPacketHeader, Arrays.stream(preInput).collect(Collectors.toList())));
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sendTime);

        stopWatch.start();
        DataPacketHeader permutationDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PERMUTATION.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        byte[] piByte = rpc.receive(permutationDataPacketHeader).getPayload().get(0);
        int[] piInvSigma = IntUtils.byteArrayToIntArray(piByte);
        byte[][] output = PermutationNetworkUtils.permutation(piInvSigma, preRosnSenderOutput.getBs());
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, receiveTime);

        logPhaseInfo(PtoState.PTO_END);
        return new DosnPartyOutput(output);
    }
}
