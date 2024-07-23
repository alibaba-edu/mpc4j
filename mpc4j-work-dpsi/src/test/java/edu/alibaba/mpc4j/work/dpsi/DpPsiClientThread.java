package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * DP-PSI client thread.
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
class DpPsiClientThread extends Thread {
    /**
     * DP-PSI client
     */
    private final DpsiClient<ByteBuffer> client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * client output
     */
    private Set<ByteBuffer> clientOutput;

    DpPsiClientThread(DpsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    Set<ByteBuffer> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            clientOutput = client.psi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
