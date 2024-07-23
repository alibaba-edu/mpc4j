package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyAidPto;

/**
 * aid PSI aider thread.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
class PsiAiderThread extends Thread {
    /**
     * aid PSI aider
     */
    private final TwoPartyAidPto aider;

    PsiAiderThread(TwoPartyAidPto aider) {
        this.aider = aider;
    }

    @Override
    public void run() {
        try {
            aider.init();
            aider.aid();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
