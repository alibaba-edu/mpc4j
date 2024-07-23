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
    /**
     * max round num
     */
    private int roundNum;

    public DirectCotSender(Rpc senderRpc, Party receiverParty, DirectCotConfig config) {
        super(DirectCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(byte[] delta, int expectNum) throws MpcAbortException {
        setInitInput(delta, expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(DirectCotPtoDesc.MAX_ROUND_NUM, expectNum);
        coreCotSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        init(delta, DirectCotPtoDesc.MAX_ROUND_NUM);
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotSenderOutput senderOutput = CotSenderOutput.createEmpty(delta);
        while (num > senderOutput.getNum()) {
            int gapNum = num - senderOutput.getNum();
            int eachNum = Math.min(gapNum, roundNum);
            senderOutput.merge(coreCotSender.send(eachNum));
        }
        stopWatch.stop();
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, roundTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    @Override
    public CotSenderOutput sendRandom(int num) throws MpcAbortException {
        return send(num);
    }
}
