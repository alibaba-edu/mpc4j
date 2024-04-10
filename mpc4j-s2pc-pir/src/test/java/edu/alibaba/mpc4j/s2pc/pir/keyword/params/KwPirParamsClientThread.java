package edu.alibaba.mpc4j.s2pc.pir.keyword.params;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * keyword PIR params client thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
public class KwPirParamsClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirParamsClientThread.class);
    /**
     * keyword PIR client
     */
    private final KwPirClient client;
    /**
     * keyword PIR params
     */
    private final KwPirParams kwPirParams;
    /**
     * label byte length
     */
    private final int labelByteLength;
    /**
     * retrieval sets
     */
    private final List<Set<ByteBuffer>> retrievalSets;
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
    /**
     * max retrieval size
     */
    private final int retrievalSize;

    KwPirParamsClientThread(KwPirClient client, KwPirParams kwPirParams, List<Set<ByteBuffer>> retrievalSets,
                            int retrievalSize, int serverElementSize, int labelByteLength) {
        this.client = client;
        this.kwPirParams = kwPirParams;
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
            client.init(kwPirParams, serverElementSize, retrievalSize, labelByteLength);
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