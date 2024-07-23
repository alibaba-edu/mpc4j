package edu.alilbaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Payable PSI server thread.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class PayablePsiServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePsiServerThread.class);
    /**
     * payable PSI server
     */
    private final PayablePsiServer server;
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

    PayablePsiServerThread(PayablePsiServer server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), clientElementSize);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
            intersectionSetSize = server.payablePsi(serverElementSet, clientElementSize);
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

    public int getIntersectionSetSize() {
        return intersectionSetSize;
    }
}