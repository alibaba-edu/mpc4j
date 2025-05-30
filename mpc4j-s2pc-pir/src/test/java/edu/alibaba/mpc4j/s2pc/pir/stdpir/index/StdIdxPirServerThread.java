package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * standard index PIR server thread with parameters.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public class StdIdxPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdIdxPirServerThread.class);
    /**
     * server
     */
    private final IdxPirServer server;
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

    public StdIdxPirServerThread(IdxPirServer server, NaiveDatabase database, int queryNum, boolean batch) {
        this.server = server;
        this.database = database;
        this.queryNum = queryNum;
        this.batch = batch;
        success = false;
    }

    public boolean getSuccess() {
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
