package edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;

import java.util.concurrent.TimeUnit;

/**
 * FIPR05 multi-query OPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Fipr05MpOprfSender extends AbstractMpOprfSender {
    /**
     * single-query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * single-query OPRF key
     */
    private SqOprfKey sqOprfKey;

    public Fipr05MpOprfSender(Rpc senderRpc, Party receiverParty, Fipr05MpOprfConfig config) {
        super(Fipr05MpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        sqOprfSender = SqOprfFactory.createSender(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(maxBatchSize, sqOprfKey);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        sqOprfSender.oprf(batchSize);
        Fipr05MpOprfSenderOutput senderOutput = new Fipr05MpOprfSenderOutput(batchSize, sqOprfKey);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, sqOprfTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
