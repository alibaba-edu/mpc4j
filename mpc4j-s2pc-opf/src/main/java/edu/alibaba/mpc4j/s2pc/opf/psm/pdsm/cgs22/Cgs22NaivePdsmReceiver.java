package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.AbstractPdsmReceiver;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 naive PDSM receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22NaivePdsmReceiver extends AbstractPdsmReceiver {
    /**
     * PESM receiver
     */
    private final PesmReceiver pesmReceiver;

    public Cgs22NaivePdsmReceiver(Rpc senderRpc, Party receiverParty, Cgs22NaivePdsmConfig config) {
        super(Cgs22NaivePdsmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        pesmReceiver = PesmFactory.createReceiver(senderRpc, receiverParty, config.getPesmConfig());
        addSubPto(pesmReceiver);
    }

    @Override
    public void init(int maxL, int maxD, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxD, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pesmReceiver.init(maxL, maxD, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector pdsm(int l, int d, byte[][] inputArray) throws MpcAbortException {
        setPtoInput(l, d, inputArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector z1 = pesmReceiver.pesm(l, d, inputArray);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }
}
