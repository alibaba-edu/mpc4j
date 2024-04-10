package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * keyword PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirServerThread.class);
    /**
     * keyword PIR server
     */
    private final KwPirServer server;
    /**
     * keyword label map
     */
    private final Map<ByteBuffer, byte[]> keywordLabelMap;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * repeat time
     */
    private final int repeatTime;

    KwPirServerThread(KwPirServer server, Map<ByteBuffer, byte[]> keywordLabelMap, int retrievalSize,
                      int labelByteLength, int repeatTime) {
        this.server = server;
        this.keywordLabelMap = keywordLabelMap;
        this.retrievalSize = retrievalSize;
        this.labelByteLength = labelByteLength;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(keywordLabelMap, retrievalSize, labelByteLength);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}