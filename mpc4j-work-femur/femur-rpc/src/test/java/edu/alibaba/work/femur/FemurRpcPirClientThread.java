package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Range keyword PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2024/9/11
 */
public class FemurRpcPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(FemurRpcPirClientThread.class);
    /**
     * range keyword PIR client
     */
    private final FemurRpcPirClient client;
    /**
     * retrieval keys
     */
    private final long[] keyArray;
    /**
     * retrieval result
     */
    private final TLongObjectMap<byte[]> retrievalResult;
    /**
     * server element size
     */
    private final int n;
    /**
     * l
     */
    private final int l;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * batch
     */
    private final boolean batch;
    /**
     * epsilon
     */
    private final double epsilon;
    /**
     * range bound
     */
    private final int rangeBound;

    FemurRpcPirClientThread(FemurRpcPirClient client, long[] keyArray, int n, int l, int rangeBound, int retrievalSize,
                            double epsilon, boolean batch) {
        this.client = client;
        this.keyArray = keyArray;
        this.n = n;
        this.l = l;
        this.rangeBound = rangeBound;
        this.retrievalSize = retrievalSize;
        retrievalResult = new TLongObjectHashMap<>();
        this.epsilon = epsilon;
        this.batch = batch;
    }

    public TLongObjectMap<byte[]> getRetrievalResult() {
        return retrievalResult;
    }

    @Override
    public void run() {
        try {
            if (batch) {
                client.init(n, l, retrievalSize);
            } else {
                client.init(n, l);
            }
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            byte[][] entries = new byte[retrievalSize][];
            if (batch) {
                entries = client.pir(keyArray, rangeBound, epsilon);
            } else {
                for (int i = 0; i < retrievalSize; i++) {
                    entries[i] = client.pir(keyArray[i], rangeBound, epsilon);
                }
            }
            for (int i = 0; i < retrievalSize; i++) {
                if (entries[i] == null) {
                    retrievalResult.put(keyArray[i], null);
                } else {
                    retrievalResult.put(keyArray[i], entries[i]);
                }
            }
            LOGGER.info(
                "Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            LOGGER.info("Client: Generated Query {}ms", client.getGenQueryTime());
            LOGGER.info("Client: Handled Response {}ms", client.getHandleResponseTime());
            client.getRpc().synchronize();
            client.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}