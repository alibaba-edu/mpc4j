package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.concurrent.TimeUnit;

/**
 * direct COT sender.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class DirectCotSender extends AbstractCotSender {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;

    public DirectCotSender(Rpc senderRpc, Party receiverParty, DirectCotConfig config) {
        super(DirectCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(byte[] delta, int updateNum) throws MpcAbortException {
        setInitInput(delta, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotSenderOutput senderOutput = CotSenderOutput.createEmpty(delta);
        if (num <= updateNum) {
            senderOutput.merge(coreCotSender.send(num));
        } else {
            int currentNum = senderOutput.getNum();
            while (currentNum < num) {
                int roundNum = Math.min((num - currentNum), updateNum);
                senderOutput.merge(coreCotSender.send(roundNum));
                currentNum = senderOutput.getNum();
            }
        }
        stopWatch.stop();
        long coreCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
