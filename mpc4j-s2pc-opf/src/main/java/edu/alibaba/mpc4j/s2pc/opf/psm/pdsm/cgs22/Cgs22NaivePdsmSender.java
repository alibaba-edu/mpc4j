package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.AbstractPdsmSender;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 naive PDSM sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22NaivePdsmSender extends AbstractPdsmSender {
    /**
     * PESM sender
     */
    private final PesmSender pesmSender;

    public Cgs22NaivePdsmSender(Rpc senderRpc, Party receiverParty, Cgs22NaivePdsmConfig config) {
        super(Cgs22NaivePdsmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        pesmSender = PesmFactory.createSender(senderRpc, receiverParty, config.getPesmConfig());
        addSubPto(pesmSender);
    }

    @Override
    public void init(int maxL, int maxD, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxD, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pesmSender.init(maxL, maxD, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector pdsm(int l, byte[][][] inputArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector z0 = pesmSender.pesm(l, inputArrays);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }
}
