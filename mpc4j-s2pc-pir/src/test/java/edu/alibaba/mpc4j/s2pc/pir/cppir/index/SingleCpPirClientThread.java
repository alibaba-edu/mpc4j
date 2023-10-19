package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

/**
 * Single client-specific preprocessing PIR client thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class SingleCpPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCpPirClientThread.class);
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * client
     */
    private final SingleCpPirClient client;
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
    private final TIntObjectMap<byte[]> retrievalResult;

    SingleCpPirClientThread(SingleCpPirClient client, int n, int l, int queryNum) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.queryNum = queryNum;
        secureRandom = new SecureRandom();
        retrievalResult = new TIntObjectHashMap<>(queryNum);
    }

    TIntObjectMap<byte[]> getRetrievalResult() {
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
                int x = secureRandom.nextInt(n);
                byte[] value = client.pir(x);
                retrievalResult.put(x, value);
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
