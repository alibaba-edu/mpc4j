package edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * FIPR05 multi-query OPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Fipr05MpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * single-quer OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;

    public Fipr05MpOprfReceiver(Rpc receiverRpc, Party senderParty, Fipr05MpOprfConfig config) {
        super(Fipr05MpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sqOprfReceiver.init(maxBatchSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(inputs);
        byte[][] prfs = IntStream.range(0, batchSize)
            .mapToObj(sqOprfReceiverOutput::getPrf)
            .toArray(byte[][]::new);
        MpOprfReceiverOutput receiverOutput = new MpOprfReceiverOutput(
            sqOprfReceiverOutput.getPrfByteLength(), inputs, prfs
        );
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, sqOprfTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
