package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * single client-specific preprocessing KSPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class SingleCpKsPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCpKsPirClientThread.class);
    /**
     * client
     */
    private final SingleCpKsPirClient<String> client;
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
     * retrieval result
     */
    private final Map<String, byte[]> retrievalResult;
    /**
     * retrieval list
     */
    private final List<String> queryList;

    SingleCpKsPirClientThread(SingleCpKsPirClient<String> client, int n, int l, List<String> queryList) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.queryList = queryList;
        this.queryNum = queryList.size();
        retrievalResult = new HashMap<>(queryNum);
    }

    public Map<String, byte[]> getRetrievalResult() {
        return retrievalResult;
    }

    @Override
    public void run() {
        try {
            client.init(n, l);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            for (int i = 0; i < queryNum; i++) {
                byte[] value = client.pir(queryList.get(i));
                if (!(value == null)) {
                    retrievalResult.put(queryList.get(i), value);
                }
            }
            LOGGER.info(
                "Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
