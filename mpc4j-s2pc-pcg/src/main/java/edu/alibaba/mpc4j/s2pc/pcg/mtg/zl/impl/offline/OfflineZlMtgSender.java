package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.offline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.AbstractZlMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgParty;

import java.util.concurrent.TimeUnit;

/**
 * offline Zl multiplication triple generator sender.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class OfflineZlMtgSender extends AbstractZlMtgParty {
    /**
     * core multiplication triple generator
     */
    private final ZlCoreMtgParty coreMtgSender;
    /**
     * max base num
     */
    private final int maxBaseNum;
    /**
     * num per round per update
     */
    private int updateRoundNum;
    /**
     * round per update
     */
    private int updateRound;
    /**
     * triple buffer
     */
    private ZlTriple tripleBuffer;

    public OfflineZlMtgSender(Rpc senderRpc, Party receiverParty, OfflineZlMtgConfig config) {
        super(OfflineZlMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ZlCoreMtgConfig coreMtgConfig = config.getCoreMtgConfig();
        coreMtgSender = ZlCoreMtgFactory.createSender(senderRpc, receiverParty, coreMtgConfig);
        addSubPto(coreMtgSender);
        maxBaseNum = coreMtgConfig.maxNum();
    }

    public OfflineZlMtgSender(Rpc senderRpc, Party receiverParty, Party aiderParty, OfflineZlMtgConfig config) {
        super(OfflineZlMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ZlCoreMtgConfig coreMtgConfig = config.getCoreMtgConfig();
        coreMtgSender = ZlCoreMtgFactory.createSender(senderRpc, receiverParty, aiderParty, coreMtgConfig);
        addSubPto(coreMtgSender);
        maxBaseNum = coreMtgConfig.maxNum();
    }

    @Override
    public void init(int updateNum) throws MpcAbortException {
        setInitInput(updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        if (updateNum <= maxBaseNum) {
            // we only need to run one round
            updateRoundNum = updateNum;
            updateRound = 1;
        } else {
            // we need to run multiple rounds
            updateRoundNum = maxBaseNum;
            updateRound = (int) Math.ceil((double) updateNum / maxBaseNum);
        }
        coreMtgSender.init(updateRoundNum);
        tripleBuffer = ZlTriple.createEmpty(zl);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        // generate triple in offline phase
        for (int round = 1; round <= updateRound; round++) {
            stopWatch.start();
            ZlTriple triple = coreMtgSender.generate(updateRoundNum);
            tripleBuffer.merge(triple);
            stopWatch.stop();
            long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logSubStepInfo(PtoState.INIT_STEP, 2, round, updateRound, roundTime);
        }

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        while (num > tripleBuffer.getNum()) {
            // generate if we do not have enough triples
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                ZlTriple triple = coreMtgSender.generate(updateRoundNum);
                tripleBuffer.merge(triple);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, 0, round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        ZlTriple senderOutput = tripleBuffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, splitTripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
