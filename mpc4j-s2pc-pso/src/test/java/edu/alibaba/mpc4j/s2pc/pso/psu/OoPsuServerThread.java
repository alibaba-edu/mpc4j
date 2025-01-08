package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Offline/Online PSU server thread.
 *
 * @author Feng Han
 * @date 2024/12/9
 */
public class OoPsuServerThread extends Thread {
    /**
     * PSU server
     */
    private final OoPsuServer server;
    /**
     * server set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client size
     */
    private final int clientElementSize;
    /**
     * element byte length
     */
    private final int elementByteLength;

    OoPsuServerThread(OoPsuServer server, Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
        this.elementByteLength = elementByteLength;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), clientElementSize);
            server.preCompute(serverElementSet.size(), clientElementSize, elementByteLength);
            server.psu(serverElementSet, clientElementSize, elementByteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
