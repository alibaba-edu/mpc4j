package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.util.concurrent.TimeUnit;

/**
 * cache COT sender.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CacheCotSender extends AbstractCotSender {
    /**
     * no-choice COT sender
     */
    private final NcCotSender ncCotSender;
    /**
     * precompute COT sender
     */
    private final PreCotSender preCotSender;
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
    private CotSenderOutput buffer;

    public CacheCotSender(Rpc senderRpc, Party receiverParty, CacheCotConfig config) {
        super(CacheCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, ncCotConfig);
        addSubPto(ncCotSender);
        maxBaseNum = ncCotConfig.maxNum();
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
    }

    @Override
    public void init(byte[] delta, int updateNum) throws MpcAbortException {
        setInitInput(delta, updateNum);
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
        ncCotSender.init(delta, perRoundNum);
        preCotSender.init();
        buffer = CotSenderOutput.createEmpty(delta);
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

        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                CotSenderOutput cotSenderOutput = ncCotSender.send();
                buffer.merge(cotSenderOutput);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, 0, round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        CotSenderOutput senderOutput = buffer.split(num);
        stopWatch.stop();
        long splitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, splitTime);

        stopWatch.start();
        // correct choices using precompute COT
        senderOutput = preCotSender.send(senderOutput);
        stopWatch.stop();
        long preCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, preCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
