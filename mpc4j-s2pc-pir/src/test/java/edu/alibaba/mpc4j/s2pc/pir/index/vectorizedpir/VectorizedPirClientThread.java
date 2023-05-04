package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;

/**
 * Vectorized PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class VectorizedPirClientThread extends Thread {
    /**
     * Vectorized PIR client
     */
    private final Mr23IndexPirClient client;
    /**
     * Vectorized PIR params
     */
    private final Mr23IndexPirParams indexPirParams;
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

    VectorizedPirClientThread(Mr23IndexPirClient client, Mr23IndexPirParams indexPirParams, int retrievalIndex,
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
