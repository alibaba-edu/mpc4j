package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.AbstractPreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotPtoDesc.PtoStep;

/**
 * Bea95 pre-compute COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Bea95PreCotReceiver extends AbstractPreCotReceiver {

    public Bea95PreCotReceiver(Rpc receiverRpc, Party senderParty, Bea95PreCotConfig config) {
        super(Bea95PreCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive(CotReceiverOutput preReceiverOutput, boolean[] choices) {
        logPhaseInfo(PtoState.PTO_BEGIN);
        setPtoInput(preReceiverOutput, choices);

        stopWatch.start();
        byte[] xors = BinaryUtils.binaryToRoundByteArray(choices);
        byte[] preChoiceBytes = BinaryUtils.binaryToRoundByteArray(preReceiverOutput.getChoices());
        BytesUtils.xori(xors, preChoiceBytes);
        List<byte[]> xorPayload = new LinkedList<>();
        xorPayload.add(xors);
        DataPacketHeader xorHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_XOR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(xorHeader, xorPayload));
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(preReceiverOutput::getRb)
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        CotReceiverOutput receiverOutput = CotReceiverOutput.create(choices, rbArray);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
