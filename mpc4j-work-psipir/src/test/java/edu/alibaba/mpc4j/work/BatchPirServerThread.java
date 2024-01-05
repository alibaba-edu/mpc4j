package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * batch PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPirServerThread.class);
    /**
     * batch PIR server
     */
    private final BatchPirServer server;
    /**
     * database
     */
    private final BitVector database;
    /**
     * max retrieval size
     */
    private final int maxRetrievalSize;

    BatchPirServerThread(BatchPirServer server, BitVector database, int maxRetrievalSize) {
        this.server = server;
        this.database = database;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    @Override
    public void run() {
        try {
            server.init(database, maxRetrievalSize);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            server.pir();
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