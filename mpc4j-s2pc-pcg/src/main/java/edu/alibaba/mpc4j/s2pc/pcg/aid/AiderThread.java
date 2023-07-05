package edu.alibaba.mpc4j.s2pc.pcg.aid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * aider thread.
 *
 * @author Weiran Liu
 * @date 2023/5/20
 */
public class AiderThread extends Thread {
    /**
     * aider
     */
    private final TrustDealAider aider;

    public AiderThread(TrustDealAider aider) {
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
