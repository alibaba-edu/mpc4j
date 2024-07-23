package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * client-specific preprocessing KSPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class CpKsPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpKsPirClientThread.class);
    /**
     * client
     */
    private final CpKsPirClient<String> client;
    /**
     * database size
     */
    private final int n;
    /**
     * value bit length
     */
    private final int l;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * retrieval keys
     */
    private final ArrayList<String> keys;
    /**
     * retrieval result
     */
    private final Map<String, byte[]> retrievalResult;
    /**
     * batch
     */
    private final boolean batch;
    /**
     * success
     */
    private boolean success;

    CpKsPirClientThread(CpKsPirClient<String> client, int n, int l, ArrayList<String> keys, boolean batch) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.keys = keys;
        this.queryNum = keys.size();
        retrievalResult = new HashMap<>(queryNum);
        this.batch = batch;
    }

    boolean getSuccess() {
        return success;
    }

    Map<String, byte[]> getRetrievalResult() {
        return retrievalResult;
    }

    @Override
    public void run() {
        try {
            if (batch) {
                client.init(n, l, queryNum);
            } else {
                client.init(n, l);
            }
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            byte[][] entries = new byte[queryNum][];
            if (batch) {
                entries = client.pir(keys);
            } else {
                for (int i = 0; i < queryNum; i++) {
                    entries[i] = client.pir(keys.get(i));
                }
            }
            for (int i = 0; i < queryNum; i++) {
                if (!(entries[i] == null)) {
                    retrievalResult.put(keys.get(i), entries[i]);
                }
            }

            LOGGER.info(
                "Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
            success = true;
        } catch (MpcAbortException e) {
            e.printStackTrace();
            success = false;
        }
    }
}
