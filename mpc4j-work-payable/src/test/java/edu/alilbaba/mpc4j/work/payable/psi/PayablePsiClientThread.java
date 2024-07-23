package edu.alilbaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Payable PSI client thread.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class PayablePsiClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePsiClientThread.class);
    /**
     * payable PSI client
     */
    private final PayablePsiClient client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * intersection set
     */
    private Set<ByteBuffer> intersectionSet;
    /**
     * server element size
     */
    private final int serverElementSize;

    PayablePsiClientThread(PayablePsiClient client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    public Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
            intersectionSet = client.payablePsi(clientElementSet, serverElementSize);
            LOGGER.info(
                "Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}