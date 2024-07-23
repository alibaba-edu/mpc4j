package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cot;

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
public class CotLnotReceiver extends AbstractLnotReceiver {
    /**
     * no-choice LNOT receiver
     */
    private final NcLnotReceiver ncLnotReceiver;
    /**
     * precompute LNOT receiver
     */
    private final PreLnotReceiver preLnotReceiver;
    /**
     * buffer
     */
    private LnotReceiverOutput buffer;

    public CotLnotReceiver(Rpc receiverRpc, Party senderParty, CotLnotConfig config) {
        super(CotLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncLnotReceiver = NcLnotFactory.createReceiver(receiverRpc, senderParty, config.getNcLnotConfig());
        addSubPto(ncLnotReceiver);
        preLnotReceiver = PreLnotFactory.createReceiver(receiverRpc, senderParty, config.getPreLnotConfig());
        addSubPto(preLnotReceiver);
    }

    @Override
    public void init(int l, int expectNum) throws MpcAbortException {
        setInitInput(l, expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(config.defaultRoundNum(l), expectNum);
        ncLnotReceiver.init(l, roundNum);
        preLnotReceiver.init();
        buffer = LnotReceiverOutput.createEmpty(l);
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

        stopWatch.start();
        while (num > buffer.getNum()) {
            LnotReceiverOutput lnotReceiverOutput = ncLnotReceiver.receive();
            buffer.merge(lnotReceiverOutput);
        }
        LnotReceiverOutput receiverOutput = buffer.split(num);
        stopWatch.stop();
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, roundTime);

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
