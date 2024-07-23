package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * standard index PIR client thread.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public class StdIdxPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdIdxPirClientThread.class);
    /**
     * client
     */
    private final IdxPirClient client;
    /**
     * database size
     */
    private final int n;
    /**
     * l
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
     * random state
     */
    private final SecureRandom secureRandom;
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

    public StdIdxPirClientThread(IdxPirClient client, int n, int l, int queryNum, boolean batch) {
        this.client = client;
        this.n = n;
        this.l = l;
        this.queryNum = queryNum;
        this.batch = batch;
        secureRandom = new SecureRandom();
        success = false;
    }

    public boolean getSuccess() {
        return success;
    }

    public int[] getXs() {
        return xs;
    }

    public byte[][] getEntries() {
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
            if (batch) {
                entries = client.pir(xs);
            } else {
                entries = new byte[queryNum][];
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
