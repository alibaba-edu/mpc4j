package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;

/**
 * XPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class XPirClientThread extends Thread {
    /**
     * XPIR client
     */
    private final Mbfk16IndexPirClient client;
    /**
     * XPIR params
     */
    private final Mbfk16IndexPirParams indexPirParams;
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

    XPirClientThread(Mbfk16IndexPirClient client, Mbfk16IndexPirParams indexPirParams, int retrievalIndex,
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
