package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * keyword PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirClientThread.class);
    /**
     * keyword PIR client
     */
    private final KwPirClient client;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * retrieval sets
     */
    private final List<Set<ByteBuffer>> retrievalSets;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * repeat time
     */
    private final int repeatTime;
    /**
     * retrieval result
     */
    private final List<Map<ByteBuffer, byte[]>> retrievalResults;
    /**
     * server element size
     */
    private final int serverElementSize;

    KwPirClientThread(KwPirClient client, List<Set<ByteBuffer>> retrievalSets, int retrievalSize, int serverElementSize,
                      int labelByteLength) {
        this.client = client;
        this.retrievalSets = retrievalSets;
        this.retrievalSize = retrievalSize;
        this.serverElementSize = serverElementSize;
        this.labelByteLength = labelByteLength;
        repeatTime = retrievalSets.size();
        retrievalResults = new ArrayList<>(repeatTime);
    }

    public Map<ByteBuffer, byte[]> getRetrievalResult(int index) {
        return retrievalResults.get(index);
    }

    @Override
    public void run() {
        try {
            client.init(retrievalSize, serverElementSize, labelByteLength);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            for (int i = 0; i < repeatTime; i++) {
                retrievalResults.add(client.pir(retrievalSets.get(i)));
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