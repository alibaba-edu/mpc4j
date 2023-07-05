package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AidPsiAider;

/**
 * aid PSI aider thread.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
class AidPsiAiderThread extends Thread {
    /**
     * aid PSI aider
     */
    private final AidPsiAider aider;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * client element size
     */
    private final int clientElementSize;

    AidPsiAiderThread(AidPsiAider aider, int serverElementSize, int clientElementSize) {
        this.aider = aider;
        this.serverElementSize = serverElementSize;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            aider.init(serverElementSize, clientElementSize);
            aider.psi(serverElementSize, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
