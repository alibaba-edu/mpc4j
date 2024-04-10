package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.AbstractLnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotSender;

import java.util.concurrent.TimeUnit;

/**
 * cache 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class CacheLnotSender extends AbstractLnotSender {
    /**
     * no-choice LNOT sender
     */
    private final NcLnotSender ncLnotSender;
    /**
     * precompute LNOT sender
     */
    private final PreLnotSender preLnotSender;
    /**
     * update round
     */
    private int updateRound;
    /**
     * buffer
     */
    private LnotSenderOutput buffer;

    public CacheLnotSender(Rpc senderRpc, Party receiverParty, CacheLnotConfig config) {
        super(CacheLnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ncLnotSender = NcLnotFactory.createSender(senderRpc, receiverParty, config.getNcLnotConfig());
        addSubPto(ncLnotSender);
        preLnotSender = PreLnotFactory.createSender(senderRpc, receiverParty, config.getPreLnotConfig());
        addSubPto(preLnotSender);
    }

    @Override
    public void init(int l, int updateNum) throws MpcAbortException {
        setInitInput(l, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int perRoundNum;
        if (updateNum <= config.maxBaseNum()) {
            // we only need to run single round
            perRoundNum = updateNum;
            updateRound = 1;
        } else {
            // we need to run multiple round
            perRoundNum = config.maxBaseNum();
            updateRound = (int) Math.ceil((double) updateNum / config.maxBaseNum());
        }
        ncLnotSender.init(l, perRoundNum);
        preLnotSender.init();
        buffer = LnotSenderOutput.createEmpty(l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                LnotSenderOutput lnotSenderOutput = ncLnotSender.send();
                buffer.merge(lnotSenderOutput);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, 0, round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        LnotSenderOutput senderOutput = buffer.split(num);
        stopWatch.stop();
        long splitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, splitTime);

        stopWatch.start();
        // correct the choice array using precompute LNOT
        senderOutput = preLnotSender.send(senderOutput);
        stopWatch.stop();
        long preLnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, preLnotTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
