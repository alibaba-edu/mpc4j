package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.DoubleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamCpIdxPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * single client-specific preprocessing index PIR test.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
@RunWith(Parameterized.class)
public class CpIdxPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_L = 32;
    /**
     * default database size
     */
    private static final int DEFAULT_N = (1 << 18) - 3;
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 105;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DOUBLE
        configurations.add(new Object[]{
            CpIdxPirType.DOUBLE.name(), new DoubleCpIdxPirConfig.Builder().build()
        });
        // PAI
        configurations.add(new Object[]{
            CpIdxPirType.PAI.name(), new PaiCpIdxPirConfig.Builder().build()
        });
        // SPAM
        configurations.add(new Object[]{
            CpIdxPirType.SPAM.name(), new SpamCpIdxPirConfig.Builder().build()
        });
        // PIANO
        configurations.add(new Object[]{
            CpIdxPirType.PIANO.name(), new PianoCpIdxPirConfig.Builder().build()
        });
        // SIMPLE
        configurations.add(new Object[]{
            CpIdxPirType.SIMPLE.name(), new SimpleCpIdxPirConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final CpIdxPirConfig config;

    public CpIdxPirTest(String name, CpIdxPirConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1n() {
        testPto(1, DEFAULT_L, 1, false);
    }

    @Test
    public void test2n() {
        testPto(2, DEFAULT_L, 1, false);
    }

    @Test
    public void testSpecificN() {
        testPto(11, DEFAULT_L, 1, false);
    }

    @Test
    public void testLargeQueryNum() {
        testPto(11, DEFAULT_L, 22, false);
    }

    @Test
    public void testSpecificValue() {
        testPto(DEFAULT_N, 11, DEFAULT_QUERY_NUM, false);
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
    public void testLargeValue() {
        testPto(DEFAULT_N, 1 << 6, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelLargeValue() {
        testPto(DEFAULT_N, 1 << 6, DEFAULT_QUERY_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(1 << 20, DEFAULT_L, 1 << 10, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1 << 20, DEFAULT_L, 1 << 10, true);
    }

    private void testPto(int n, int l, int queryNum, boolean parallel) {
        testPto(n, l, queryNum, false, parallel);
        testPto(n, l, queryNum, true, parallel);
    }

    public void testPto(int n, int l, int queryNum, boolean batch, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        byte[][] dataByteArrays = IntStream.range(0, n)
            .parallel()
            .mapToObj(x -> Arrays.copyOf(IntUtils.intToByteArray(x), byteL))
            .toArray(byte[][]::new);
        NaiveDatabase database = NaiveDatabase.create(l, dataByteArrays);
        CpIdxPirServer server = CpIdxPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        CpIdxPirClient client = CpIdxPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        CpIdxPirServerThread serverThread = new CpIdxPirServerThread(server, database, queryNum, batch);
        CpIdxPirClientThread clientThread = new CpIdxPirClientThread(client, n, l, queryNum, batch);
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
