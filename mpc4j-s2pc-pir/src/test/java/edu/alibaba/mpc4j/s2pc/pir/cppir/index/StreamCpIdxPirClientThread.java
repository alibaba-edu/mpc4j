package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * stream client-specific preprocessing index PIR client thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class StreamCpIdxPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamCpIdxPirClientThread.class);
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * client
     */
    private final StreamCpIdxPirClient client;
    /**
     * database size
     */
    private final int n;
    /**
     * value bit length
     */
    private final int l;
    /**
     * update indexes
     */
    private final int[] updateIndexes;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * xs
     */
    private int[] xs;
    /**
     * entries
     */
    private byte[][] entries;
    /**
     * success
     */
    private boolean success;

    StreamCpIdxPirClientThread(StreamCpIdxPirClient client, int n, int l, int[] updateIndexes, int queryNum) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.updateIndexes = updateIndexes;
        this.queryNum = queryNum;
        secureRandom = new SecureRandom();
        success = false;
    }

    boolean getSuccess() {
        return success;
    }

    int[] getXs() {
        return xs;
    }

    byte[][] getEntries() {
        return entries;
    }

    @Override
    public void run() {
        try {
            client.init(n, l, queryNum);
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            // update database
            client.update(updateIndexes.length);

            // query
            xs = IntStream.range(0, queryNum).map(i -> {
                if (i % 2 == 0) {
                    // half queries are updated indexes
                    return updateIndexes[secureRandom.nextInt(updateIndexes.length)];
                } else {
                    // half queries are random indexes
                    return secureRandom.nextInt(n);
                }
            }).toArray();
            entries = new byte[queryNum][];
            entries = client.pir(xs);
            LOGGER.info(
                "Client: The Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            success = true;
        } catch (MpcAbortException e) {
            e.printStackTrace();
            success = false;
        }
    }
}
