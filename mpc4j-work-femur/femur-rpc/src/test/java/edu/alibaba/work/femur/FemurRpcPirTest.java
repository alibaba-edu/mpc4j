package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.work.femur.naive.NaiveFemurRpcPirConfig;
import edu.alibaba.work.femur.seal.SealFemurRpcPirConfig;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Range keyword PIR test.
 *
 * @author Lqiaing Peng
 * @date 2024/9/11
 */
@RunWith(Parameterized.class)
public class FemurRpcPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * small server element size
     */
    private static final int SMALL_SERVER_ELEMENT_SIZE = 1 << 10;
    /**
     * default server element size
     */
    private static final int DEFAULT_SERVER_ELEMENT_SIZE = 1 << 14;
    /**
     * large server element size
     */
    private static final int LARGE_SERVER_ELEMENT_SIZE = 1 << 18;
    /**
     * default retrieval size
     */
    private static final int DEFAULT_RETRIEVAL_SIZE = 1 << 4;
    /**
     * default value length
     */
    private static final int DEFAULT_VALUE_BIT_LENGTH = Long.SIZE;
    /**
     * default range bound
     */
    private static final int DEFAULT_RANGE_BOUND = 1 << 10;
    /**
     * small epsilon
     */
    private static final double SMALL_EPSILON = 0.1;
    /**
     * default epsilon
     */
    private static final double DEFAULT_EPSILON = 0.001;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PGM-index range SEAL PIR
        configurations.add(new Object[]{
            FemurRpcPirFactory.FemurPirType.PGM_INDEX_SEAL_PIR.name(),
            new SealFemurRpcPirConfig.Builder().build()
        });

        // PGM-index range SEAL PIR + differential privacy
        configurations.add(new Object[]{
            FemurRpcPirFactory.FemurPirType.PGM_INDEX_SEAL_PIR.name() + " + dp",
            new SealFemurRpcPirConfig.Builder().setDp(true).build()
        });

        // PGM-index range naive PIR
        configurations.add(new Object[]{
            FemurRpcPirFactory.FemurPirType.PGM_INDEX_NAIVE_PIR.name(),
            new NaiveFemurRpcPirConfig.Builder().build()
        });

        // PGM-index range naive PIR + differential privacy
        configurations.add(new Object[]{
            FemurRpcPirFactory.FemurPirType.PGM_INDEX_NAIVE_PIR.name() + " + dp",
            new NaiveFemurRpcPirConfig.Builder().setDp(true).build()
        });

        return configurations;
    }

    /**
     * range keyword PIR config
     */
    private final FemurRpcPirConfig config;

    public FemurRpcPirTest(String name, FemurRpcPirConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefaultElementSizeParallel() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_VALUE_BIT_LENGTH, DEFAULT_EPSILON, true);
    }

    @Test
    public void testDefaultElementSize() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_VALUE_BIT_LENGTH, DEFAULT_EPSILON, false);
    }

    @Test
    public void testLargeElementSize() {
        testPto(LARGE_SERVER_ELEMENT_SIZE, DEFAULT_VALUE_BIT_LENGTH, DEFAULT_EPSILON, true);
    }

    @Test
    public void testSmallElementSize() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, DEFAULT_VALUE_BIT_LENGTH, DEFAULT_EPSILON, true);
    }

    @Test
    public void testLargeValue() {
        testPto(LARGE_SERVER_ELEMENT_SIZE, 1 << 8, DEFAULT_EPSILON, true);
    }

    @Test
    public void testSpecialElementSize() {
        testPto((1 << 15) + 7, DEFAULT_VALUE_BIT_LENGTH, DEFAULT_EPSILON, true);
    }

    @Test
    public void testSmallEpsilon() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_VALUE_BIT_LENGTH, SMALL_EPSILON, true);
    }

    private void testPto(int n, int l, double epsilon, boolean parallel) {
        testPto(n, l, FemurRpcPirTest.DEFAULT_RANGE_BOUND, FemurRpcPirTest.DEFAULT_RETRIEVAL_SIZE, epsilon, false, parallel);
        testPto(n, l, FemurRpcPirTest.DEFAULT_RANGE_BOUND, FemurRpcPirTest.DEFAULT_RETRIEVAL_SIZE, epsilon, true, parallel);
    }

    public void testPto(int n, int l, int rangeBound, int retrievalSize, double epsilon, boolean batch, boolean parallel) {
        TLongObjectMap<byte[]> keyValueMap = generateKeyValueDatabase(n, l, SECURE_RANDOM);
        long[] retrievalKeyArray = generateRetrievalKeyArray(keyValueMap.keySet(), retrievalSize);
        // create instance
        FemurRpcPirServer server = FemurRpcPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        FemurRpcPirClient client = FemurRpcPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        FemurRpcPirServerThread serverThread = new FemurRpcPirServerThread(server, keyValueMap, l, retrievalSize, batch);
        FemurRpcPirClientThread clientThread = new FemurRpcPirClientThread(
            client, retrievalKeyArray, n, l, rangeBound, retrievalSize, epsilon, batch
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify
            TLongObjectMap<byte[]> clientOutput = clientThread.getRetrievalResult();
            Assert.assertEquals(retrievalSize, clientOutput.size());
            Arrays.stream(clientOutput.keys(), 0, retrievalSize)
                .forEach(key -> Assert.assertArrayEquals(keyValueMap.get(key), clientOutput.get(key)));
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private long[] generateRetrievalKeyArray(TLongSet keySet, int retrievalSize) {
        List<Long> retrivalList = new ArrayList<>();
        long[] retrievalArray = new long[retrievalSize];
        long[] keyArray = keySet.toArray();
        int keySize = keyArray.length;
        for (int i = 0; i < retrievalSize; i++) {
            if (SECURE_RANDOM.nextBoolean()) {
                long key;
                do {
                    int index = SECURE_RANDOM.nextInt(keySize);
                    key = keyArray[index];
                } while (retrivalList.contains(key));
                retrievalArray[i] = key;
                retrivalList.add(key);
            } else {
                do {
                    retrievalArray[i] = Math.abs(SECURE_RANDOM.nextLong());
                } while (keySet.contains(retrievalArray[i]));
            }
        }
        return retrievalArray;
    }

    /**
     * Generate key-value database.
     *
     * @param size         database size.
     * @param l            entry bit length.
     * @param secureRandom random state.
     * @return key-value database.
     */
    public static TLongObjectMap<byte[]> generateKeyValueDatabase(int size, int l, SecureRandom secureRandom) {
        assert l % Long.SIZE == 0;
        int byteL = CommonUtils.getByteLength(l);
        TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>(size);
        for (int i = 0; i < size; i++) {
            long key;
            byte[] entry;
            do {
                key = secureRandom.nextLong();
                entry = BytesUtils.randomByteArray(byteL, l, secureRandom);
            } while (keyValueMap.containsKey(key));
            keyValueMap.put(key, entry);
        }
        assert keyValueMap.size() == size;
        return keyValueMap;
    }
}