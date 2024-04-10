package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * payable PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class PayablePirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePirClientThread.class);
    /**
     * payable PIR client
     */
    private final PayablePirClient client;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * retrieval key
     */
    private final ByteBuffer retrievalKey;
    /**
     * client output
     */
    private byte[] clientOutput;
    /**
     * server element size
     */
    private final int serverElementSize;

    PayablePirClientThread(PayablePirClient client, ByteBuffer retrievalKey, int serverElementSize, int labelByteLength) {
        this.client = client;
        this.retrievalKey = retrievalKey;
        this.serverElementSize = serverElementSize;
        this.labelByteLength = labelByteLength;
    }

    public byte[] getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(serverElementSize, labelByteLength);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            clientOutput = client.pir(retrievalKey);
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