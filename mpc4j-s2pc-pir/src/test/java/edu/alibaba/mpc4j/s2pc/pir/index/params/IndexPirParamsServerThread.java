package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index PIR params server thread.
 *
 * @author Weiran Liu
 * @date 2023/6/29
 */
public class IndexPirParamsServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexPirParamsServerThread.class);
    /**
     * index PIR server
     */
    private final SingleIndexPirServer server;
    /**
     * index PIR params
     */
    private final SingleIndexPirParams indexPirParams;
    /**
     * database
     */
    private final NaiveDatabase database;

    IndexPirParamsServerThread(SingleIndexPirServer server, SingleIndexPirParams indexPirParams, NaiveDatabase database) {
        this.server = server;
        this.indexPirParams = indexPirParams;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            server.init(indexPirParams, database);
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
