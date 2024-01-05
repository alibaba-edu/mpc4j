package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * batch PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPirClientThread.class);
    /**
     * batch PIR client
     */
    private final BatchPirClient client;
    /**
     * retrieval index list
     */
    private final List<Integer> retrievalIndexList;
    /**
     * retrieval result
     */
    private Map<Integer, Boolean> retrievalResult;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * max retrieval size
     */
    private final int maxRetrievalSize;

    BatchPirClientThread(BatchPirClient client, List<Integer> retrievalIndexList, int serverElementSize,
                         int maxRetrievalSize) {
        this.client = client;
        this.retrievalIndexList = retrievalIndexList;
        this.retrievalResult = new HashMap<>(retrievalIndexList.size());
        this.serverElementSize = serverElementSize;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    public Map<Integer, Boolean> getRetrievalResult() {
        return retrievalResult;
    }

    @Override
    public void run() {
        try {
            client.init(serverElementSize, maxRetrievalSize);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            retrievalResult = client.pir(retrievalIndexList);
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