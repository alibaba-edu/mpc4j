package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;

/**
 * SEAL PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class SealPirClientThread extends Thread {
    /**
     * SEAL PIR client
     */
    private final Acls18IndexPirClient client;
    /**
     * SEAL PIR params
     */
    private final Acls18IndexPirParams indexPirParams;
    /**
     * element byte length
     */
    private final int elementByteLength;
    /**
     * retrieval index
     */
    private final int retrievalIndex;
    /**
     * database size
     */
    private final int serverElementSize;
    /**
     * retrieval result
     */
    private ByteBuffer indexPirResult;

    SealPirClientThread(Acls18IndexPirClient client, Acls18IndexPirParams indexPirParams, int retrievalIndex,
                        int serverElementSize, int elementByteLength) {
        this.client = client;
        this.indexPirParams = indexPirParams;
        this.retrievalIndex = retrievalIndex;
        this.serverElementSize = serverElementSize;
        this.elementByteLength = elementByteLength;
    }

    public ByteBuffer getRetrievalResult() {
        return indexPirResult;
    }

    @Override
    public void run() {
        try {
            client.init(indexPirParams, serverElementSize, elementByteLength);
            client.getRpc().synchronize();
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalIndex));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}