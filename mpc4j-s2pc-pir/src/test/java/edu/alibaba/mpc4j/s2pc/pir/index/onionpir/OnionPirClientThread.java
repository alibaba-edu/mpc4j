package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;

/**
 * OnionPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class OnionPirClientThread extends Thread {
    /**
     * OnionPIR client
     */
    private final Mcr21IndexPirClient client;
    /**
     * OnionPIR params
     */
    private final Mcr21IndexPirParams indexPirParams;
    /**
     * element byte length
     */
    private final int elementByteLength;
    /**
     * retrieval index
     */
    private final int retrievalIndex;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * retrieval result
     */
    private ByteBuffer indexPirResult;

    OnionPirClientThread(Mcr21IndexPirClient client, Mcr21IndexPirParams indexPirParams, int retrievalIndex,
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
