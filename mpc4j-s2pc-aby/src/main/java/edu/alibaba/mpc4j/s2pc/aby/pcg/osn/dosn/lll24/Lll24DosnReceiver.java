package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.AbstractDosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24 Decision OSN receiver.
 *
 * @author Feng Han
 * @date 2024/6/20
 */
public class Lll24DosnReceiver extends AbstractDosnReceiver {
    /**
     * CST Random OSN
     */
    private final RosnReceiver rosnReceiver;

    public Lll24DosnReceiver(Rpc receiverRpc, Party senderParty, Lll24DosnConfig config) {
        super(Lll24DosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
        rosnReceiver = RosnFactory.createReceiver(receiverRpc, senderParty, config.getRosnConfig());
        addSubPto(rosnReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rosnReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public DosnPartyOutput dosn(int[] pi, int byteLength) throws MpcAbortException {
        setPtoInput(pi, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        RosnReceiverOutput receiverOutput = rosnReceiver.rosn(pi, byteLength);
        stopWatch.stop();
        long rosnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, rosnTime);

        stopWatch.start();
        byte[][] delta = innerOsn(receiverOutput);
        stopWatch.stop();
        long maskInputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, maskInputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new DosnPartyOutput(delta);
    }

    @Override
    public DosnPartyOutput dosn(int[] pi, int byteLength, RosnReceiverOutput receiverOutput) throws MpcAbortException {
        Preconditions.checkArgument(Arrays.equals(pi, receiverOutput.getPi()));
        Preconditions.checkArgument(receiverOutput.getByteLength() == byteLength);
        setPtoInput(pi, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] delta = innerOsn(receiverOutput);
        stopWatch.stop();
        long maskInputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, maskInputTime);

        logPhaseInfo(PtoState.PTO_END);
        return new DosnPartyOutput(delta);
    }

    private byte[][] innerOsn(RosnReceiverOutput receiverOutput) throws MpcAbortException {
        List<byte[]> maskInputDataPacketPayload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_MASK_INPUT.ordinal());
        MpcAbortPreconditions.checkArgument(maskInputDataPacketPayload.size() == num);
        byte[][] maskInputBytes = maskInputDataPacketPayload.toArray(new byte[0][]);
        byte[][] delta = receiverOutput.getDeltas();
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        intStream.forEach(i -> BytesUtils.xori(delta[i], maskInputBytes[pi[i]]));
        return delta;
    }
}
