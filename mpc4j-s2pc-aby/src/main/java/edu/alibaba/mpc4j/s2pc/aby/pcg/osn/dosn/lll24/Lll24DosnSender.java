package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.AbstractDosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LLL24 Decision OSN sender.
 *
 * @author Feng Han
 * @date 2024/6/20
 */
public class Lll24DosnSender extends AbstractDosnSender {
    /**
     * Random OSN sender
     */
    private final RosnSender rosnSender;

    public Lll24DosnSender(Rpc senderRpc, Party receiverParty, Lll24DosnConfig config) {
        super(Lll24DosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
        rosnSender = RosnFactory.createSender(senderRpc, receiverParty, config.getRosnConfig());
        addSubPto(rosnSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rosnSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public DosnPartyOutput dosn(byte[][] inputVector, int byteLength) throws MpcAbortException {
        setPtoInput(inputVector, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        RosnSenderOutput senderOutput = rosnSender.rosn(num, byteLength);
        stopWatch.stop();
        long rosnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, rosnTime);

        stopWatch.start();
        // P1 also sends m = x + a^(1)
        byte[][] as = senderOutput.getAs();
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        List<byte[]> maskInputDataPacketPayload = intStream
            .mapToObj(index -> BytesUtils.xor(inputVector[index], as[index]))
            .collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SENDER_SEND_MASK_INPUT.ordinal(), maskInputDataPacketPayload);
        stopWatch.stop();
        long maskInputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, maskInputTime);

        return new DosnPartyOutput(senderOutput.getBs());
    }
}
