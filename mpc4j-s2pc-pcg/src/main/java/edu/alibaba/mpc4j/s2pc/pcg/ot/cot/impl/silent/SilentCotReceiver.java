package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.silent;

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
public class SilentCotReceiver extends AbstractCotReceiver {
    /**
     * no-choice COT receiver
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * precompute COT receiver
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * max round num
     */
    private final int maxRoundNum;
    /**
     * buffer
     */
    private CotReceiverOutput buffer;

    public SilentCotReceiver(Rpc receiverRpc, Party senderParty, SilentCotConfig config) {
        super(SilentCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, ncCotConfig);
        addSubPto(ncCotReceiver);
        maxRoundNum = ncCotConfig.maxNum();
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        ncCotReceiver.init(roundNum);
        buffer = CotReceiverOutput.createEmpty();
        preCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init() throws MpcAbortException {
        init(maxRoundNum);
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
            buffer.merge(cotReceiverOutput);
        }
        stopWatch.stop();
        CotReceiverOutput receiverOutput = buffer.split(num);
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, roundTime);

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

    @Override
    public CotReceiverOutput receiveRandom(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
            buffer.merge(cotReceiverOutput);
        }
        CotReceiverOutput receiverOutput = buffer.split(num);
        stopWatch.stop();
        long splitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, splitTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
