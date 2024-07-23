package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * standard KSPIR client thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class StdKsPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdKsPirClientThread.class);
    /**
     * keyword PIR client
     */
    private final StdKsPirClient<ByteBuffer> client;
    /**
     * label bit length
     */
    private final int l;
    /**
     * retrieval keys
     */
    private final ArrayList<ByteBuffer> keys;
    /**
     * retrieval size
     */
    private final int retrievalSize;
    /**
     * retrieval result
     */
    private byte[][] entries;
    /**
     * server element size
     */
    private final int n;

    public StdKsPirClientThread(StdKsPirClient<ByteBuffer> client, ArrayList<ByteBuffer> keys, int n, int l) {
        this.client = client;
        this.keys = keys;
        this.retrievalSize = keys.size();
        this.n = n;
        this.l = l;
        entries = new byte[retrievalSize][];
    }

    public byte[][] getRetrievalResult() {
        return entries;
    }

    @Override
    public void run() {
        try {
            client.init(n, l, retrievalSize);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            entries = client.pir(keys);
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