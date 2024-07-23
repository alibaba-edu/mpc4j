package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.silent;

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
 * silent COT sender.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class SilentCotSender extends AbstractCotSender {
    /**
     * NC-COT sender
     */
    private final NcCotSender ncCotSender;
    /**
     * precompute COT sender
     */
    private final PreCotSender preCotSender;
    /**
     * max round num
     */
    private final int maxRoundNum;
    /**
     * buffer
     */
    private CotSenderOutput buffer;

    public SilentCotSender(Rpc senderRpc, Party receiverParty, SilentCotConfig config) {
        super(SilentCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, ncCotConfig);
        addSubPto(ncCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
        maxRoundNum = ncCotConfig.maxNum();
    }

    @Override
    public void init(byte[] delta, int expectNum) throws MpcAbortException {
        setInitInput(delta, expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        ncCotSender.init(delta, roundNum);
        preCotSender.init();
        buffer = CotSenderOutput.createEmpty(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        init(delta, maxRoundNum);
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            CotSenderOutput cotSenderOutput = ncCotSender.send();
            buffer.merge(cotSenderOutput);
        }
        CotSenderOutput senderOutput = buffer.split(num);
        stopWatch.stop();
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, roundTime);

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

    @Override
    public CotSenderOutput sendRandom(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            CotSenderOutput cotSenderOutput = ncCotSender.send();
            buffer.merge(cotSenderOutput);
        }
        CotSenderOutput senderOutput = buffer.split(num);
        stopWatch.stop();
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, roundTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
