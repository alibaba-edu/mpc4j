package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.MirPlinkoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.PianoPlinkoCpIdxPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * stream single client-specific preprocessing index PIR test.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
@RunWith(Parameterized.class)
public class StreamCpIdxPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_L = 16;
    /**
     * default database size
     */
    private static final int DEFAULT_N = (1 << 14) - 3;
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 15;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MIR
        configurations.add(new Object[]{
            CpIdxPirType.MIR.name(), new MirCpIdxPirConfig.Builder().build(),
        });
        // PIANO
        configurations.add(new Object[]{
            CpIdxPirType.PIANO.name(), new PianoCpIdxPirConfig.Builder().build(),
        });
        // MIR_PLINKO
        configurations.add(new Object[]{
            CpIdxPirType.MIR_PLINKO.name() + "(default Q)",
            new MirPlinkoCpIdxPirConfig.Builder().build(),
        });
        configurations.add(new Object[]{
            CpIdxPirType.MIR_PLINKO.name() + "(specific Q)",
            new MirPlinkoCpIdxPirConfig.Builder().setQ(1 << 12).build(),
        });
        // PIANO_PLINKO
        configurations.add(new Object[]{
            CpIdxPirType.PIANO_PLINKO.name() + "(default Q)",
            new PianoPlinkoCpIdxPirConfig.Builder().build(),
        });
        configurations.add(new Object[]{
            CpIdxPirType.PIANO_PLINKO.name() + "(specific Q)",
            new PianoPlinkoCpIdxPirConfig.Builder().setQ(1 << 12).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final CpIdxPirConfig config;

    public StreamCpIdxPirTest(String name, CpIdxPirConfig config) {
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
        int n = 1 << 8;
        int q = CpIdxPirFactory.supportRoundQueryNum(config.getPtoType(), n);
        if (q == Integer.MAX_VALUE) {
            q = (int) Math.sqrt(n);
        }
        testPto(n, DEFAULT_L, q * 10, false);
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
        testPto(1 << 6, (1 << 7) + 2, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelLargeValue() {
        testPto(DEFAULT_N, 1 << 6, DEFAULT_QUERY_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(1 << 17, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1 << 17, DEFAULT_L, DEFAULT_QUERY_NUM, true);
    }

    private void testPto(int n, int l, int queryNum, boolean parallel) {
        int updateNum = 4 * (int) Math.ceil(Math.sqrt(n));
        int byteL = CommonUtils.getByteLength(l);
        byte[][] dataByteArrays = BytesUtils.randomByteArrayVector(n, byteL, l, SECURE_RANDOM);
        NaiveDatabase database = NaiveDatabase.create(l, dataByteArrays);
        // update half of the database, can be duplicated
        int[] updateIndexes = IntStream.range(0, updateNum)
            .map(i -> SECURE_RANDOM.nextInt(n))
            .toArray();
        byte[][] updateEntries = BytesUtils.randomByteArrayVector(updateNum, byteL, l, SECURE_RANDOM);
        StreamCpIdxPirServer server = CpIdxPirFactory.createUpdatableServer(firstRpc, secondRpc.ownParty(), config);
        StreamCpIdxPirClient client = CpIdxPirFactory.createStreamClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        StreamCpIdxPirServerThread serverThread = new StreamCpIdxPirServerThread(
            server, database, updateIndexes, updateEntries, queryNum
        );
        StreamCpIdxPirClientThread clientThread = new StreamCpIdxPirClientThread(client, n, l, updateIndexes, queryNum);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Assert.assertTrue(serverThread.getSuccess());
            Assert.assertTrue(clientThread.getSuccess());
            // verify entries after updates
            for (int i = 0; i < updateNum; i++) {
                database.setBytesData(updateIndexes[i], updateEntries[i]);
            }
            int[] xs = clientThread.getXs();
            byte[][] queryEntries = clientThread.getEntries();
            for (int i = 0; i < queryNum; i++) {
                int x = xs[i];
                byte[] expect = database.getBytesData(x);
                byte[] actual = queryEntries[i];
                Assert.assertArrayEquals("The " + i + "-th result is not correct", expect, actual);
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
