package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;

/**
 * XPIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class XPirServerThread extends Thread {
    /**
     * XPIR server
     */
    private final Mbfk16IndexPirServer server;
    /**
     * XPIR params
     */
    private final Mbfk16IndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    XPirServerThread(Mbfk16IndexPirServer server, Mbfk16IndexPirParams indexPirParams, NaiveDatabase database) {
        this.server = server;
        this.indexPirParams = indexPirParams;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(indexPirParams, database);
            server.getRpc().synchronize();
            server.pir();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
