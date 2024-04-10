package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.util.concurrent.TimeUnit;

/**
 * cache COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CacheCotReceiver extends AbstractCotReceiver {
    /**
     * no-choice COT receiver
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * precompute COT receiver
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * max base num
     */
    private final int maxBaseNum;
    /**
     * update round
     */
    private int updateRound;
    /**
     * buffer
     */
    private CotReceiverOutput buffer;

    public CacheCotReceiver(Rpc receiverRpc, Party senderParty, CacheCotConfig config) {
        super(CacheCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, ncCotConfig);
        addSubPto(ncCotReceiver);
        maxBaseNum = ncCotConfig.maxNum();
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
    }

    @Override
    public void init(int updateNum) throws MpcAbortException {
        setInitInput(updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int perRoundNum;
        if (updateNum <= maxBaseNum) {
            // we only need to run single round
            perRoundNum = updateNum;
            updateRound = 1;
        } else {
            // we need to run multiple round
            perRoundNum = maxBaseNum;
            updateRound = (int) Math.ceil((double) updateNum / maxBaseNum);
        }
        ncCotReceiver.init(perRoundNum);
        buffer = CotReceiverOutput.createEmpty();
        preCotReceiver.init();
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

        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
                buffer.merge(cotReceiverOutput);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, 0, round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        CotReceiverOutput receiverOutput = buffer.split(num);
        stopWatch.stop();
        long splitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, splitTime);

        stopWatch.start();
        // correct choices using precompute COT
        receiverOutput = preCotReceiver.receive(receiverOutput, choices);
        stopWatch.stop();
        long preCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, preCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
