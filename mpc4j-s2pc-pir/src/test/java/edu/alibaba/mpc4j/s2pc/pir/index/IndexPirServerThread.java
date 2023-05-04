package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;

/**
 * Index PIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class IndexPirServerThread extends Thread {
    /**
     * index PIR server
     */
    private final IndexPirServer server;
    /**
     * database
     */
    private final NaiveDatabase database;

    IndexPirServerThread(IndexPirServer server, NaiveDatabase database) {
        this.server = server;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(database);
            server.getRpc().synchronize();
            server.pir();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
