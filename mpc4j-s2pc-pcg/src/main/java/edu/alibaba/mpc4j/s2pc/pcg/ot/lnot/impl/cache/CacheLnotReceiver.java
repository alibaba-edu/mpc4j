package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.AbstractLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotReceiver;

import java.util.concurrent.TimeUnit;

/**
 * cache 1-out-of-n (with n = 2^l) receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class CacheLnotReceiver extends AbstractLnotReceiver {
    /**
     * no-choice LNOT receiver
     */
    private final NcLnotReceiver ncLnotReceiver;
    /**
     * precompute LNOT receiver
     */
    private final PreLnotReceiver preLnotReceiver;
    /**
     * update round
     */
    private int updateRound;
    /**
     * buffer
     */
    private LnotReceiverOutput buffer;

    public CacheLnotReceiver(Rpc receiverRpc, Party senderParty, CacheLnotConfig config) {
        super(CacheLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncLnotReceiver = NcLnotFactory.createReceiver(receiverRpc, senderParty, config.getNcLnotConfig());
        addSubPto(ncLnotReceiver);
        preLnotReceiver = PreLnotFactory.createReceiver(receiverRpc, senderParty, config.getPreLnotConfig());
        addSubPto(preLnotReceiver);
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
        ncLnotReceiver.init(l, perRoundNum);
        buffer = LnotReceiverOutput.createEmpty(l);
        preLnotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotReceiverOutput receive(int[] choiceArray) throws MpcAbortException {
        setPtoInput(choiceArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        while (num > buffer.getNum()) {
            // generate COT when we do not have enough ones
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                LnotReceiverOutput lnotReceiverOutput = ncLnotReceiver.receive();
                buffer.merge(lnotReceiverOutput);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, 0, round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        LnotReceiverOutput receiverOutput = buffer.split(num);
        stopWatch.stop();
        long splitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, splitTime);

        stopWatch.start();
        // correct the choice array using precompute LNOT
        receiverOutput = preLnotReceiver.receive(receiverOutput, choiceArray);
        stopWatch.stop();
        long preCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, preCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
