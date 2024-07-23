package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * DP-PSI server thread.
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
class DpPsiServerThread extends Thread {
    /**
     * DP-PSI server
     */
    private final DpsiServer<ByteBuffer> server;
    /**
     * server element set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;

    DpPsiServerThread(DpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), clientElementSize);
            server.psi(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}