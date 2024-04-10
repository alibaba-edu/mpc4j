package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * payable PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class PayablePirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePirServerThread.class);
    /**
     * payable PIR server
     */
    private final PayablePirServer server;
    /**
     * keyword label map
     */
    private final Map<ByteBuffer, byte[]> keywordLabelMap;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * server output
     */
    private boolean serverOutput;

    PayablePirServerThread(PayablePirServer server, Map<ByteBuffer, byte[]> keywordLabelMap, int labelByteLength) {
        this.server = server;
        this.keywordLabelMap = keywordLabelMap;
        this.labelByteLength = labelByteLength;
    }

    @Override
    public void run() {
        try {
            server.init(keywordLabelMap, labelByteLength);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            serverOutput = server.pir();
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

    public boolean getServerOutput() {
        return serverOutput;
    }
}