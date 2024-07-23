package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * single client-specific preprocessing KSPIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class CpKsPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpKsPirServerThread.class);
    /**
     * server
     */
    private final CpKsPirServer<String> server;
    /**
     * key-value map
     */
    private final Map<String, byte[]> keyValueMap;
    /**
     * value bit length
     */
    private final int l;
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

    CpKsPirServerThread(CpKsPirServer<String> server, Map<String, byte[]> keyValueMap, int l, int queryNum, boolean batch) {
        this.server = server;
        this.keyValueMap = keyValueMap;
        this.l = l;
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
                server.init(keyValueMap, l, queryNum);
            } else {
                server.init(keyValueMap, l);
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
