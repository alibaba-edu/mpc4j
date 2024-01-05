package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single client-specific preprocessing PIR server thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class SingleCpPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCpPirServerThread.class);
    /**
     * server
     */
    private final SingleCpPirServer server;
    /**
     * database
     */
    private final ZlDatabase database;
    /**
     * query num
     */
    private final int queryNum;

    SingleCpPirServerThread(SingleCpPirServer server, ZlDatabase database, int queryNum) {
        this.server = server;
        this.database = database;
        this.queryNum = queryNum;
    }

    @Override
    public void run() {
        try {
            server.init(database);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            for (int i = 0; i < queryNum; i++) {
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
