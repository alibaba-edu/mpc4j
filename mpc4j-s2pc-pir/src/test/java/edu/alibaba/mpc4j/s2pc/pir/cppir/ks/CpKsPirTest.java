package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * single client-specific preprocessing KSPIR test.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
@RunWith(Parameterized.class)
public class CpKsPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default l
     */
    private static final int DEFAULT_L = Long.SIZE;
    /**
     * default database size
     */
    private static final int DEFAULT_N = (1 << 12) - 3;
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PGM-index
        configurations.add(new Object[]{
           CpKsPirType.PGM_INDEX.name(), new SimplePgmCpKsPirConfig.Builder().build()
        });
        // simple bin
        configurations.add(new Object[]{
            CpKsPirType.SIMPLE_BIN.name(), new SimpleBinCpKsPirConfig.Builder().build()
        });
        // simple naive
        configurations.add(new Object[]{
            CpKsPirType.SIMPLE_NAIVE.name(), new SimpleNaiveCpKsPirConfig.Builder().build()
        });
        // PAI (CKS)
        configurations.add(new Object[]{
            CpKsPirType.PAI_CKS.name(), new PaiCpCksPirConfig.Builder().build()
        });
        // ALPR21 + PAI
        configurations.add(new Object[]{
            CpKsPirType.ALPR21.name() + "(" + CpIdxPirFactory.CpIdxPirType.PAI + ")",
            new Alpr21CpKsPirConfig.Builder()
                .setCpIdxPirConfig(new PaiCpIdxPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + MIR
        configurations.add(new Object[]{
            CpKsPirType.ALPR21.name() + "(" + CpIdxPirFactory.CpIdxPirType.MIR + ")",
            new Alpr21CpKsPirConfig.Builder()
                .setCpIdxPirConfig(new MirCpIdxPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + PIANO
        configurations.add(new Object[]{
            CpKsPirType.ALPR21.name() + "(" + CpIdxPirFactory.CpIdxPirType.PIANO + ")",
            new Alpr21CpKsPirConfig.Builder()
                .setCpIdxPirConfig(new PianoCpIdxPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + SIMPLE
        configurations.add(new Object[]{
            CpKsPirType.ALPR21.name() + "(" + CpIdxPirFactory.CpIdxPirType.SIMPLE + ")",
            new Alpr21CpKsPirConfig.Builder()
                .setCpIdxPirConfig(new SimpleCpIdxPirConfig.Builder().build())
                .build()
        });
        // Chalamet
        configurations.add(new Object[]{
            CpKsPirType.CHALAMET.name(), new ChalametCpKsPirConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final CpKsPirConfig config;

    public CpKsPirTest(String name, CpKsPirConfig config) {
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
        testPto(DEFAULT_N, 1 << 10, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelLargeValue() {
        testPto(DEFAULT_N, 1 << 10, DEFAULT_QUERY_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(1 << 16, DEFAULT_L, 1 << 6, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1 << 16, DEFAULT_L, 1 << 6, true);
    }

    private void testPto(int n, int l, int queryNum, boolean parallel) {
        testPto(n, l, queryNum, false, parallel);
        testPto(n, l, queryNum, true, parallel);
    }

    public void testPto(int n, int l, int queryNum, boolean batch, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        Map<String, byte[]> keywordValueMap = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toMap(
                String::valueOf,
                index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM)
            ));
        ArrayList<String> keywordArrayList = new ArrayList<>(keywordValueMap.keySet());
        ArrayList<String> queryList = IntStream.range(0, queryNum)
            .mapToObj(index -> {
                if (index % 3 == 0) {
                    return keywordArrayList.get(index % n);
                } else {
                    return  "dummy_" + index;
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));
        CpKsPirServer<String> server = CpKsPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        CpKsPirClient<String> client = CpKsPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        CpKsPirServerThread serverThread = new CpKsPirServerThread(server, keywordValueMap, l, queryNum, batch);
        CpKsPirClientThread clientThread = new CpKsPirClientThread(client, n, l, queryList, batch);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Assert.assertTrue(serverThread.getSuccess());
            Assert.assertTrue(clientThread.getSuccess());
            Map<String, byte[]> retrievalResult = clientThread.getRetrievalResult();
            for (String x : queryList) {
                if (keywordValueMap.containsKey(x)) {
                    Assert.assertArrayEquals(keywordValueMap.get(x), retrievalResult.get(x));
                } else {
                    Assert.assertNull(retrievalResult.get(x));
                }
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
