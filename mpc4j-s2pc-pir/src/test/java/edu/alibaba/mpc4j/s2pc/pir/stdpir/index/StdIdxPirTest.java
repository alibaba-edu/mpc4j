package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory.StdIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * standard index PIR test.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
@RunWith(Parameterized.class)
public class StdIdxPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * small l
     */
    private static final int SMALL_L = 9;
    /**
     * default l
     */
    private static final int DEFAULT_L = Double.SIZE;
    /**
     * large l
     */
    private static final int LARGE_L = CommonConstants.BLOCK_BIT_LENGTH - 1;
    /**
     * small n
     */
    private static final int SMALL_N = (1 << 5) - 1;
    /**
     * small n
     */
    private static final int DEFAULT_N = 1 << 10;
    /**
     * large n
     */
    private static final int LARGE_N = (1 << 14) - 1;
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 3;
    /**
     * special query num
     */
    private static final int LARGE_QUERY_NUM = (1 << 5) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        // XPIR
//        configurations.add(new Object[]{
//            StdIdxPirType.XPIR.name(), new XpirStdIdxPirConfig.Builder().build()
//        });

        // SEAL PIR
        configurations.add(new Object[]{
                StdIdxPirType.SEAL.name(), new SealStdIdxPirConfig.Builder().build()
        });

//        // Mul PIR
//        configurations.add(new Object[]{
//            StdIdxPirType.MUL.name(), new MulStdIdxPirConfig.Builder().build()
//        });

//        // Onion PIR
//        configurations.add(new Object[]{
//            StdIdxPirType.ONION.name(), new OnionStdIdxPirConfig.Builder().build()
//        });

//        // Vectorized PIR
//        configurations.add(new Object[]{
//            StdIdxPirType.VECTOR.name(), new VectorizedStdIdxPirConfig.Builder().build()
//        });

//        // Fast PIR
//        configurations.add(new Object[]{
//            StdIdxPirType.FAST.name(), new FastStdIdxPirConfig.Builder().build()
//        });

//        // constant weight PIR
//        configurations.add(new Object[]{
//            StdIdxPirType.CW.name(), new CwStdIdxPirConfig.Builder().build()
//        });

//        // PBC index PIR
//        configurations.add(new Object[]{
//            StdIdxPirType.PBC.name(), new PbcStdIdxPirConfig.Builder().build()
//        });

        return configurations;
    }

    /**
     * config
     */
    private final StdIdxPirConfig config;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public StdIdxPirTest(String name, StdIdxPirConfig config) {
        super(name);
        this.config = config;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_N, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_N, DEFAULT_L, DEFAULT_QUERY_NUM, true);
    }

    @Test
    public void test1n() {
        testPto(1, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void test2n() {
        testPto(2, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testSmallN() {
        testPto(SMALL_N, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void test1QueryNum() {
        testPto(DEFAULT_N, DEFAULT_L, 1, false);
    }

    @Test
    public void testLargeQueryNum() {
        testPto(DEFAULT_N, DEFAULT_L, LARGE_QUERY_NUM, false);
    }

    @Test
    public void testSmallL() {
        testPto(DEFAULT_N, SMALL_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testLargeL() {
        testPto(DEFAULT_N, LARGE_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_N, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_N, DEFAULT_L, DEFAULT_QUERY_NUM, true);
    }

    private void testPto(int n, int l, int queryNum, boolean parallel) {
        testPto(n, l, queryNum, false, parallel);
        testPto(n, l, queryNum, true, parallel);
    }

    public void testPto(int n, int l, int queryNum, boolean batch, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        IdxPirServer server = StdIdxPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        IdxPirClient client = StdIdxPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        StdIdxPirServerThread serverThread = new StdIdxPirServerThread(server, database, queryNum, batch);
        StdIdxPirClientThread clientThread = new StdIdxPirClientThread(client, n, l, queryNum, batch);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Assert.assertTrue(serverThread.getSuccess());
            Assert.assertTrue(clientThread.getSuccess());
            int[] xs = clientThread.getXs();
            byte[][] entries = clientThread.getEntries();
            for (int i = 0; i < queryNum; i++) {
                int x = xs[i];
                byte[] expect = database.getBytesData(x);
                byte[] actual = entries[i];
                Assert.assertArrayEquals(expect, actual);
            }
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
            System.gc();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}