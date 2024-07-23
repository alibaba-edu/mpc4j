package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * standard KSPIR server thread.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class StdKsPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdKsPirServerThread.class);
    /**
     * keyword PIR server
     */
    private final StdKsPirServer<ByteBuffer> server;
    /**
     * key-value map
     */
    private final Map<ByteBuffer, byte[]> keyValueMap;
    /**
     * label bit length
     */
    private final int l;
    /**
     * retrieval size
     */
    private final int retrievalSize;

    public StdKsPirServerThread(StdKsPirServer<ByteBuffer> server, Map<ByteBuffer, byte[]> keyValueMap, int l, int retrievalSize) {
        this.server = server;
        this.keyValueMap = keyValueMap;
        this.l = l;
        this.retrievalSize = retrievalSize;
    }

    @Override
    public void run() {
        try {
            server.init(keyValueMap, l, retrievalSize);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            server.pir(retrievalSize);
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}