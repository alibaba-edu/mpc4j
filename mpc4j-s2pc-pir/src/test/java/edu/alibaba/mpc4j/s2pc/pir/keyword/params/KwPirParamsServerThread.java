package edu.alibaba.mpc4j.s2pc.pir.keyword.params;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * keyword PIR params server thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
public class KwPirParamsServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirParamsServerThread.class);
    /**
     * keyword PIR server
     */
    private final KwPirServer server;
    /**
     * keyword PIR params
     */
    private final KwPirParams kwPirParams;
    /**
     * keyword label map
     */
    private final Map<ByteBuffer, byte[]> keywordLabelMap;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * max retrieval size
     */
    private final int retrievalSize;
    /**
     * repeat time
     */
    private final int repeatTime;

    KwPirParamsServerThread(KwPirServer server, KwPirParams kwPirParams, Map<ByteBuffer, byte[]> keywordLabelMap,
                            int retrievalSize, int labelByteLength, int repeatTime) {
        this.server = server;
        this.kwPirParams = kwPirParams;
        this.keywordLabelMap = keywordLabelMap;
        this.labelByteLength = labelByteLength;
        this.retrievalSize = retrievalSize;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(kwPirParams, keywordLabelMap, retrievalSize, labelByteLength);
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