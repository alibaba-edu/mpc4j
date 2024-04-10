package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.concurrent.TimeUnit;

/**
 * direct COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class DirectCotReceiver extends AbstractCotReceiver {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;

    public DirectCotReceiver(Rpc receiverRpc, Party senderParty, DirectCotConfig config) {
        super(DirectCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int updateNum) throws MpcAbortException {
        setInitInput(updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init(updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotReceiverOutput receiverOutput = CotReceiverOutput.createEmpty();
        if (num <= updateNum) {
            receiverOutput.merge(coreCotReceiver.receive(choices));
        } else {
            int currentNum = receiverOutput.getNum();
            int round = 0;
            while (currentNum < num) {
                int roundNum = Math.min((num - currentNum), updateNum);
                boolean[] roundChoices = new boolean[roundNum];
                System.arraycopy(choices, round * updateNum, roundChoices, 0, roundNum);
                receiverOutput.merge(coreCotReceiver.receive(roundChoices));
                round++;
                currentNum = receiverOutput.getNum();
            }
        }
        stopWatch.stop();
        long coreCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
