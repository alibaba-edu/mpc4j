package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single client-specific preprocessing index PIR server thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class CpIdxPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpIdxPirServerThread.class);
    /**
     * server
     */
    private final CpIdxPirServer server;
    /**
     * database
     */
    private final NaiveDatabase database;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * batch
     */
    private final boolean batch;
    /**
     * success
     */
    private boolean success;

    CpIdxPirServerThread(CpIdxPirServer server, NaiveDatabase database, int queryNum, boolean batch) {
        this.server = server;
        this.database = database;
        this.queryNum = queryNum;
        this.batch = batch;
        success = false;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            if (batch) {
                server.init(database, queryNum);
            } else {
                server.init(database);
            }
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            if (batch) {
                server.pir(queryNum);
            } else {
                for (int i = 0; i < queryNum; i++) {
                    server.pir();
                }
            }

            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
            success = true;
        } catch (MpcAbortException e) {
            e.printStackTrace();
            success = false;
        }
    }
}
