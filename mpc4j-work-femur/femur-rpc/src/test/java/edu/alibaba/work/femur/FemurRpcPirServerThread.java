package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import gnu.trove.map.TLongObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Range keyword PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2024/9/11
 */
public class FemurRpcPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(FemurRpcPirServerThread.class);
    /**
     * range keyword PIR server
     */
    private final FemurRpcPirServer server;
    /**
     * key value map
     */
    private final TLongObjectMap<byte[]> keyValueMap;
    /**
     * l
     */
    private final int l;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * batch
     */
    private final boolean batch;

    FemurRpcPirServerThread(FemurRpcPirServer server, TLongObjectMap<byte[]> keyValueMap, int l, int retrievalSize,
                            boolean batch) {
        this.server = server;
        this.keyValueMap = keyValueMap;
        this.l = l;
        this.retrievalSize = retrievalSize;
        this.batch = batch;
    }

    @Override
    public void run() {
        try {
            if (batch) {
                server.init(keyValueMap, l, retrievalSize);
            } else {
                server.init(keyValueMap, l);
            }
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            if (batch) {
                server.pir(retrievalSize);
            } else {
                for (int i = 0; i < retrievalSize; i++) {
                    server.pir();
                }
            }
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            LOGGER.info("Server: Generated Response {}ms", server.getGenResponseTime());
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}