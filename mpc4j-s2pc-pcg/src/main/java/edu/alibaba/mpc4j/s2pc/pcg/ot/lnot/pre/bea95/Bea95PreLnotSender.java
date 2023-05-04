package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.AbstractPreLnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotPtoDesc.PtoStep;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Bea95 pre-compute 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Bea95PreLnotSender extends AbstractPreLnotSender {

    public Bea95PreLnotSender(Rpc senderRpc, Party receiverParty, Bea95PreLnotConfig config) {
        super(Bea95PreLnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotSenderOutput send(LnotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(preSenderOutput);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> deltaPayload = rpc.receive(deltaHeader).getPayload();
        int[] deltas = handleDeltaPayload(deltaPayload);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] shiftRsArray = indexIntStream
            .mapToObj(index -> {
                byte[][] rs = preSenderOutput.getRs(index);
                int delta = deltas[index];
                // shift rs into the correct position
                byte[][] shiftRs = new byte[n][];
                for (int choice = 0; choice < n; choice++) {
                    int shiftPosition = choice - delta;
                    shiftPosition = shiftPosition < 0 ? shiftPosition + n : shiftPosition;
                    shiftRs[choice] = BytesUtils.clone(rs[shiftPosition]);
                }
                return shiftRs;
            })
            .toArray(byte[][][]::new);
        LnotSenderOutput senderOutput = LnotSenderOutput.create(l, shiftRsArray);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private int[] handleDeltaPayload(List<byte[]> deltaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(deltaPayload.size() == 1);
        byte[] flatDeltas = deltaPayload.remove(0);
        int deltaLength = IntUtils.boundedNonNegIntByteLength(n);
        return IntStream.range(0, num)
            .map(index -> {
                byte[] deltaBytes = new byte[deltaLength];
                System.arraycopy(flatDeltas, index * deltaLength, deltaBytes, 0, deltaLength);
                return IntUtils.byteArrayToBoundedNonNegInt(deltaBytes, n);
            })
            .toArray();
    }
}
