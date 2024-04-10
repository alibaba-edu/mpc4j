package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory.SingleCpPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirFactory.SingleCpKsPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * single client-specific preprocessing KSPIR test.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
@RunWith(Parameterized.class)
public class SingleCpKsPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_L = Long.SIZE;
    /**
     * default database size
     */
    private static final int DEFAULT_N = (1 << 16) - 3;
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PAI (CKS)
        configurations.add(new Object[]{
            SingleCpKsPirType.PAI_CKS.name(),
            new PaiSingleCpCksPirConfig.Builder().build()
        });
        // ALPR21 + PAI
        configurations.add(new Object[]{
            SingleCpKsPirType.ALPR21.name() + "(" + SingleCpPirType.PAI + ")",
            new Alpr21SingleCpKsPirConfig.Builder()
                .setSingleIndexCpPirConfig(new PaiSingleCpPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + SPAM
        configurations.add(new Object[]{
            SingleCpKsPirType.ALPR21.name() + "(" + SingleCpPirType.SPAM + ")",
            new Alpr21SingleCpKsPirConfig.Builder()
                .setSingleIndexCpPirConfig(new SpamSingleCpPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + PIANO
        configurations.add(new Object[]{
            SingleCpKsPirType.ALPR21.name() + "(" + SingleCpPirType.PIANO + ")",
            new Alpr21SingleCpKsPirConfig.Builder()
                .setSingleIndexCpPirConfig(new PianoSingleCpPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + SIMPLE
        configurations.add(new Object[]{
            SingleCpKsPirType.ALPR21.name() + "(" + SingleCpPirType.SIMPLE + ")",
            new Alpr21SingleCpKsPirConfig.Builder()
                .setSingleIndexCpPirConfig(new SimpleSingleCpPirConfig.Builder().build())
                .build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final SingleCpKsPirConfig config;

    public SingleCpKsPirTest(String name, SingleCpKsPirConfig config) {
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
        testPto(1 << 18, DEFAULT_L, 1 << 6, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1 << 18, DEFAULT_L, 1 << 6, true);
    }

    public void testPto(int n, int l, int queryNum, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        Map<String, byte[]> keywordValueMap = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toMap(
                String::valueOf,
                index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM)
            ));
        ArrayList<String> keywordArrayList = new ArrayList<>(keywordValueMap.keySet());
        List<String> queryList = IntStream.range(0, queryNum)
            .mapToObj(index -> {
                if (index % 3 == 0) {
                    return keywordArrayList.get(index % n);
                } else {
                    return  "dummy_" + index;
                }
            })
            .collect(Collectors.toList());
        SingleCpKsPirServer<String> server = SingleCpKsPirFactory.createServer(
            firstRpc, secondRpc.ownParty(), config
        );
        SingleCpKsPirClient<String> client = SingleCpKsPirFactory.createClient(
            secondRpc, firstRpc.ownParty(), config
        );
        server.setParallel(parallel);
        client.setParallel(parallel);
        SingleCpKsPirServerThread serverThread = new SingleCpKsPirServerThread(server, keywordValueMap, l, queryNum);
        SingleCpKsPirClientThread clientThread = new SingleCpKsPirClientThread(client, n, l, queryList);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
