package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * stream client-specific preprocessing index PIR server thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class StreamCpIdxPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamCpIdxPirServerThread.class);
    /**
     * server
     */
    private final StreamCpIdxPirServer server;
    /**
     * database
     */
    private final NaiveDatabase database;
    /**
     * update indexes
     */
    private final int[] updateIndexes;
    /**
     * update entries
     */
    private final byte[][] updateEntries;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * success
     */
    private boolean success;

    StreamCpIdxPirServerThread(StreamCpIdxPirServer server, NaiveDatabase database,
                               int[] updateIndexes, byte[][] updateEntries, int queryNum) {
        this.server = server;
        this.database = database;
        // same size for indexes and entries
        assert updateIndexes.length == updateEntries.length;
        this.updateIndexes = updateIndexes;
        this.updateEntries = updateEntries;
        this.queryNum = queryNum;
        success = false;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            server.init(database, queryNum);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            // update the database
            server.update(updateIndexes, updateEntries);

            // query
            server.pir(queryNum);
            success = true;
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
            success = false;
        }
    }
}
