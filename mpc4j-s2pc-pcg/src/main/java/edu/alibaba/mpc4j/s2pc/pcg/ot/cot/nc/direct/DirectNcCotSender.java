package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.concurrent.TimeUnit;

/**
 * direct no-choice COT sender.
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class DirectNcCotSender extends AbstractNcCotSender {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;

    public DirectNcCotSender(Rpc senderRpc, Party receiverParty, DirectNcCotConfig config) {
        super(DirectNcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotSenderOutput senderOutput = coreCotSender.send(num);
        stopWatch.stop();
        long coreCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
