package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * client-specific preprocessing index PIR client thread.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
class CpIdxPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpIdxPirClientThread.class);
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * client
     */
    private final CpIdxPirClient client;
    /**
     * database size
     */
    private final int n;
    /**
     * value bit length
     */
    private final int l;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * batch
     */
    private final boolean batch;
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

    CpIdxPirClientThread(CpIdxPirClient client, int n, int l, int queryNum, boolean batch) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.queryNum = queryNum;
        secureRandom = new SecureRandom();
        this.batch = batch;
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
            if (batch) {
                client.init(n, l, queryNum);
            } else {
                client.init(n, l);
            }
            LOGGER.info(
                "Client: The Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();

            xs = IntStream.range(0, queryNum).map(i -> secureRandom.nextInt(n)).toArray();
            entries = new byte[queryNum][];
            // query
            if (batch) {
                entries = client.pir(xs);
            } else {
                for (int i = 0; i < queryNum; i++) {
                    entries[i] = client.pir(xs[i]);
                }
            }
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
