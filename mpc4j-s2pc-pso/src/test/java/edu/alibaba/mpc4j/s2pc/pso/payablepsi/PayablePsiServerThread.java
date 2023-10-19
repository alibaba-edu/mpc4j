package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Payable PSI server thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
class PayablePsiServerThread extends Thread {
    /**
     * payable PSI server
     */
    private final PayablePsiServer<ByteBuffer> server;
    /**
     * server element set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * intersection set size
     */
    private int intersectionSetSize;

    PayablePsiServerThread(PayablePsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), clientElementSize);
            intersectionSetSize = server.psi(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

    public int getIntersectionSetSize() {
        return intersectionSetSize;
    }
}
