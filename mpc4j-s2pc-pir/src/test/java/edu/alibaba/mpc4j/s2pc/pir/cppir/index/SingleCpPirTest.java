package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory.SingleCpPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirConfig;
import gnu.trove.map.TIntObjectMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * single client-specific preprocessing PIR test.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
@RunWith(Parameterized.class)
public class SingleCpPirTest extends AbstractTwoPartyMemoryRpcPto {
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

        // PAI
        configurations.add(new Object[]{
            SingleCpPirType.PAI.name(), new PaiSingleCpPirConfig.Builder().build()
        });
        // SPAM
        configurations.add(new Object[]{
            SingleCpPirType.SPAM.name(), new SpamSingleCpPirConfig.Builder().build()
        });
        // PIANO
        configurations.add(new Object[]{
            SingleCpPirType.PIANO.name(), new PianoSingleCpPirConfig.Builder().build()
        });
        // SIMPLE
        configurations.add(new Object[]{
            SingleCpPirType.SIMPLE.name(), new SimpleSingleCpPirConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final SingleCpPirConfig config;

    public SingleCpPirTest(String name, SingleCpPirConfig config) {
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

    public void testPto(int n, int l, int queryNum, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        byte[][] dataByteArrays = IntStream.range(0, n)
            .parallel()
            .mapToObj(x -> Arrays.copyOf(IntUtils.intToByteArray(x), byteL))
            .toArray(byte[][]::new);
        ZlDatabase database = ZlDatabase.create(l, dataByteArrays);
        SingleCpPirServer server = SingleCpPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        SingleCpPirClient client = SingleCpPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        SingleCpPirServerThread serverThread = new SingleCpPirServerThread(server, database, queryNum);
        SingleCpPirClientThread clientThread = new SingleCpPirClientThread(client, n, l, queryNum);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            TIntObjectMap<byte[]> retrievalResult = clientThread.getRetrievalResult();
            for (int x : retrievalResult.keys()) {
                Assert.assertArrayEquals(retrievalResult.get(x), database.getBytesData(x));
            }
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
