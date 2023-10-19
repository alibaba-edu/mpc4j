package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * single client-specific preprocessing KSPIR server thread.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
class SingleCpKsPirServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCpKsPirServerThread.class);
    /**
     * server
     */
    private final SingleCpKsPirServer<String> server;
    /**
     * keyword label map
     */
    private final Map<String, byte[]> keywordValueMap;
    /**
     * value bit length
     */
    private final int valueBitLength;
    /**
     * query num
     */
    private final int queryNum;

    SingleCpKsPirServerThread(SingleCpKsPirServer<String> server,
                              Map<String, byte[]> keywordValueMap, int valueBitLength, int queryNum) {
        this.server = server;
        this.keywordValueMap = keywordValueMap;
        this.valueBitLength = valueBitLength;
        this.queryNum = queryNum;
    }

    @Override
    public void run() {
        try {
            server.init(keywordValueMap, valueBitLength);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().synchronize();
            server.getRpc().reset();

            for (int i = 0; i < queryNum; i++) {
                server.pir();
            }
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
