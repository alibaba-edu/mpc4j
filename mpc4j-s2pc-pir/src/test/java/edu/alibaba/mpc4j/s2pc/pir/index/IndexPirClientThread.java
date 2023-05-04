package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;

/**
 * Index PIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class IndexPirClientThread extends Thread {
    /**
     * index PIR client
     */
    private final IndexPirClient client;
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

    IndexPirClientThread(IndexPirClient client, int retrievalIndex, int serverElementSize, int elementByteLength) {
        this.client = client;
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
            client.init(serverElementSize, elementByteLength);
            client.getRpc().synchronize();
            indexPirResult = ByteBuffer.wrap(client.pir(retrievalIndex));
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
