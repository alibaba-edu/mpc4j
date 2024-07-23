package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.AbstractZlWrapParty;

import java.util.concurrent.TimeUnit;

/**
 * RRKC20 Zl wrap protocol receiver.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrkc20ZlWrapReceiver extends AbstractZlWrapParty {
    /**
     * millionaire receiver.
     */
    private final MillionaireParty millionaireParty;

    public Rrkc20ZlWrapReceiver(Z2cParty z2cReceiver, Party senderParty, Rrkc20ZlWrapConfig config) {
        super(Rrkc20ZlWrapPtoDesc.getInstance(), z2cReceiver.getRpc(), senderParty, config);
        millionaireParty = MillionaireFactory.createReceiver(z2cReceiver, senderParty, config.getMillionaireConfig());
        addSubPto(millionaireParty);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        millionaireParty.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector wrap(int l, byte[][] ys) throws MpcAbortException {
        setPtoInput(l, ys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector z1 = millionaireParty.lt(l, ys);
        stopWatch.stop();
        long compareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, compareTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }
}
