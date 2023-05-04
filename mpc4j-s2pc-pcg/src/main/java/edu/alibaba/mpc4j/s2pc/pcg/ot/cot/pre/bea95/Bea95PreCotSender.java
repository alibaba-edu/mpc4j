package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.AbstractPreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotPtoDesc.PtoStep;

/**
 * Bea95 pre-compute COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Bea95PreCotSender extends AbstractPreCotSender {

    public Bea95PreCotSender(Rpc senderRpc, Party receiverParty, Bea95PreCotConfig config) {
        super(Bea95PreCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send(CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(preSenderOutput);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader xorHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_XOR.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> xorPayload = rpc.receive(xorHeader).getPayload();
        MpcAbortPreconditions.checkArgument(xorPayload.size() == 1);
        byte[] xors = xorPayload.remove(0);
        int offset = CommonUtils.getByteLength(num) * Byte.SIZE - num;
        byte[][] r0Array = IntStream.range(0, num)
            // switch the position if xor = 1
            .mapToObj(index -> BinaryUtils.getBoolean(xors, index + offset) ?
                preSenderOutput.getR1(index) : BytesUtils.clone(preSenderOutput.getR0(index)))
            .toArray(byte[][]::new);
        CotSenderOutput senderOutput = CotSenderOutput.create(preSenderOutput.getDelta(), r0Array);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
