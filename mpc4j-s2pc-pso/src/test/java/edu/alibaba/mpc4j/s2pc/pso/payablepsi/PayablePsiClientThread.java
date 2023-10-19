package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Payable PSI client thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
class PayablePsiClientThread extends Thread {
    /**
     * payable PSI client
     */
    private final PayablePsiClient<ByteBuffer> client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * intersection set
     */
    private Set<ByteBuffer> intersectionSet;

    PayablePsiClientThread(PayablePsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            intersectionSet = client.psi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
