package edu.alibaba.work.femur.demo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoRedisPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoRedisPirConfig;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Femur demo PIR test.
 *
 * @author Weiran Liu
 * @date 2024/12/3
 */
@RunWith(Parameterized.class)
public class FemurDemoPirTest {
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 4;
    /**
     * default t
     */
    private static final int DEFAULT_T = 1 << 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Naive Femur Redis PIR
        configurations.add(new Object[]{
            FemurDemoPirType.NAIVE_REDIS.name(), new NaiveFemurDemoRedisPirConfig.Builder().build()
        });
        configurations.add(new Object[]{
            FemurDemoPirType.NAIVE_REDIS.name() + " (DP)", new NaiveFemurDemoRedisPirConfig.Builder().setDp(true).build()
        });
        // Naive Femur Demo PIR
        configurations.add(new Object[]{
            FemurDemoPirType.NAIVE_MEMORY.name(), new NaiveFemurDemoMemoryPirConfig.Builder().build()
        });
        configurations.add(new Object[]{
            FemurDemoPirType.NAIVE_MEMORY.name() + " (DP)", new NaiveFemurDemoMemoryPirConfig.Builder().setDp(true).build()
        });
        // SEAL Femur Redis PIR
        configurations.add(new Object[]{
            FemurDemoPirType.SEAL_REDIS.name(), new SealFemurDemoRedisPirConfig.Builder().build()
        });
        configurations.add(new Object[]{
            FemurDemoPirType.SEAL_REDIS.name() + " (DP)", new SealFemurDemoRedisPirConfig.Builder().setDp(true).build()
        });
        // SEAL Femur Demo PIR
        configurations.add(new Object[]{
            FemurDemoPirType.SEAL_MEMORY.name(), new SealFemurDemoMemoryPirConfig.Builder().build()
        });
        configurations.add(new Object[]{
            FemurDemoPirType.SEAL_MEMORY.name() + " (DP)", new SealFemurDemoMemoryPirConfig.Builder().setDp(true).build()
        });

        return configurations;
    }

    /**
     * range keyword PIR config
     */
    private final FemurDemoPirConfig config;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public FemurDemoPirTest(String name, FemurDemoPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testStatus() {
        int n = 1 << 10;
        int l = Long.SIZE;
        FemurDemoPirServer server = FemurDemoPirFactory.createServer(config);
        FemurDemoPirClient client = FemurDemoPirFactory.createClient(config);

        // query without init
        List<byte[]> registerRequestPayload = client.register("Alice");
        Pair<FemurStatus, List<byte[]>> registerResponse = server.register(registerRequestPayload);
        Assert.assertEquals(registerResponse.getLeft(), FemurStatus.SERVER_NOT_INIT);
        Assert.assertEquals(registerResponse.getRight().size(), 0);
        Pair<FemurStatus, List<byte[]>> hintResponse = server.getHint();
        Assert.assertEquals(hintResponse.getLeft(), FemurStatus.SERVER_NOT_INIT);
        Assert.assertEquals(hintResponse.getRight().size(), 0);

        server.init(n, l);
        // query without database
        registerResponse = server.register(registerRequestPayload);
        Assert.assertEquals(registerResponse.getLeft(), FemurStatus.SERVER_NOT_KVDB);
        Assert.assertEquals(registerResponse.getRight().size(), 0);
        hintResponse = server.getHint();
        Assert.assertEquals(hintResponse.getLeft(), FemurStatus.SERVER_NOT_KVDB);
        Assert.assertEquals(hintResponse.getRight().size(), 0);
    }

    @Test
    public void testDatabaseSize() {
        testPto(1 << 10, Long.SIZE, 0.01);
        testPto(1 << 14, Long.SIZE, 0.01);
        testPto(1 << 20, Long.SIZE, 0.01);
        testPto((1 << 15) + 7, Long.SIZE, 0.01);
    }

    @Test
    public void testEntryLength() {
        testPto(1 << 14, Long.SIZE, 0.01);
        testPto(1 << 14, 2 * Long.SIZE, 0.01);
        testPto(1 << 14, 1 << 10, 0.01);
        testPto(1 << 14, 1 << 12, 0.01);
    }

    @Test
    public void testEpsilon() {
        testPto(1 << 14, Long.SIZE, 0.1);
        testPto(1 << 14, Long.SIZE, 0.01);
        testPto(1 << 14, Long.SIZE, 0.001);
    }

    private void testPto(int n, int l, double epsilon) {
        TLongObjectMap<byte[]> keyValueDatabase = generateKeyValueDatabase(n, l, secureRandom);
        FemurDemoPirServer server = FemurDemoPirFactory.createServer(config);
        FemurDemoPirClient client = FemurDemoPirFactory.createClient(config);
        // init and set database
        server.init(n, l);
        server.setDatabase(keyValueDatabase);
        // client register
        List<byte[]> registerRequestPayload = client.register("Alice");
        Pair<FemurStatus, List<byte[]>> registerResponse = server.register(registerRequestPayload);
        Assert.assertEquals(registerResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setDatabaseParams(registerResponse.getRight());
        // query for hint
        Pair<FemurStatus, List<byte[]>> hintResponse = server.getHint();
        Assert.assertEquals(hintResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setHint(hintResponse.getRight());
        // query
        long[] keys = keyValueDatabase.keys();
        for (int i = 0; i < DEFAULT_QUERY_NUM; i++) {
            long key = keys[secureRandom.nextInt(n)];
            List<byte[]> queryPayload = client.query(key, DEFAULT_T, epsilon);
            Pair<FemurStatus, List<byte[]>> responsePayload = server.response(queryPayload);
            Pair<FemurStatus, byte[]> answer = client.retrieve(responsePayload);
            Assert.assertEquals(answer.getKey(), FemurStatus.SERVER_SUCC_RES);
            Assert.assertArrayEquals(answer.getValue(), keyValueDatabase.get(key));
        }
        // query keys that are not in the key-value database
        for (int i = 0; i < DEFAULT_QUERY_NUM; i++) {
            long randomKey = secureRandom.nextLong();
            while (keyValueDatabase.containsKey(randomKey)) {
                randomKey = secureRandom.nextLong();
            }
            List<byte[]> queryPayload = client.query(randomKey, DEFAULT_T, epsilon);
            Pair<FemurStatus, List<byte[]>> responsePayload = server.response(queryPayload);
            Pair<FemurStatus, byte[]> answer = client.retrieve(responsePayload);
            Assert.assertEquals(answer.getKey(), FemurStatus.SERVER_SUCC_RES);
            Assert.assertNull(answer.getValue());
        }
        server.reset();
    }

    @Test
    public void testUpdateValue() {
        testUpdateValue(1 << 10, Long.SIZE, 0.01);
        testUpdateValue(1 << 14, Long.SIZE * 2, 0.1);
        testUpdateValue(1 << 18, Long.SIZE, 0.001);
        testUpdateValue(1 << 10, 1 << 10, 0.01);
        testUpdateValue(1 << 14, 1 << 10, 0.1);
        testUpdateValue(1 << 18, 1 << 10, 0.001);
    }

    private void testUpdateValue(int n, int l, double epsilon) {
        TLongObjectMap<byte[]> keyValueDatabase = generateKeyValueDatabase(n, l, secureRandom);
        FemurDemoPirServer server = FemurDemoPirFactory.createServer(config);
        FemurDemoPirClient client = FemurDemoPirFactory.createClient(config);
        // init and set database
        server.init(n, l);
        server.setDatabase(keyValueDatabase);
        // client register
        List<byte[]> registerRequestPayload = client.register("Alice");
        Pair<FemurStatus, List<byte[]>> registerResponse = server.register(registerRequestPayload);
        Assert.assertEquals(registerResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setDatabaseParams(registerResponse.getRight());
        // query for hint
        Pair<FemurStatus, List<byte[]>> hintResponse = server.getHint();
        Assert.assertEquals(hintResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setHint(hintResponse.getRight());
        // query
        long[] keys = keyValueDatabase.keys();
        for (int i = 0; i < DEFAULT_QUERY_NUM; i++) {
            int keyIndex = secureRandom.nextInt(n);
            long key = keys[keyIndex];
            List<byte[]> queryPayload = client.query(key, DEFAULT_T, epsilon);
            Pair<FemurStatus, List<byte[]>> responsePayload = server.response(queryPayload);
            Pair<FemurStatus, byte[]> answer = client.retrieve(responsePayload);
            Assert.assertEquals(answer.getKey(), FemurStatus.SERVER_SUCC_RES);
            Assert.assertArrayEquals(answer.getValue(), keyValueDatabase.get(key));
            // update this value
            int byteL = CommonUtils.getByteLength(l);
            byte[] updateValue = BytesUtils.randomByteArray(byteL, l, secureRandom);
            Assert.assertTrue(server.updateValue(key, updateValue));
            keyValueDatabase.put(key, updateValue);
            // query again
            queryPayload = client.query(key, DEFAULT_T, epsilon);
            responsePayload = server.response(queryPayload);
            answer = client.retrieve(responsePayload);
            Assert.assertEquals(answer.getKey(), FemurStatus.SERVER_SUCC_RES);
            Assert.assertArrayEquals(answer.getValue(), keyValueDatabase.get(key));
        }
        server.reset();
    }

    @Test
    public void testUpdateDatabase() {
        testUpdateDatabase(1 << 10, Long.SIZE, 0.01);
        testUpdateDatabase(1 << 14, Long.SIZE * 2, 0.1);
        testUpdateDatabase(1 << 6, Long.SIZE, 0.001);
    }

    private void testUpdateDatabase(int n, int l, double epsilon) {
        TLongObjectMap<byte[]> oldKeyValueDatabase = generateKeyValueDatabase(n, l, secureRandom);
        FemurDemoPirServer server = FemurDemoPirFactory.createServer(config);
        FemurDemoPirClient client = FemurDemoPirFactory.createClient(config);
        // init and set database
        server.init(n, l);
        server.setDatabase(oldKeyValueDatabase);
        // client register
        List<byte[]> registerRequestPayload = client.register("Alice");
        Pair<FemurStatus, List<byte[]>> registerResponse = server.register(registerRequestPayload);
        Assert.assertEquals(registerResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setDatabaseParams(registerResponse.getRight());
        // query for hint
        Pair<FemurStatus, List<byte[]>> hintResponse = server.getHint();
        Assert.assertEquals(hintResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setHint(hintResponse.getRight());
        // update database
        TLongObjectMap<byte[]> newKeyValueDatabase = generateKeyValueDatabase(n, l, secureRandom);
        server.setDatabase(newKeyValueDatabase);
        // query with old hint
        long[] oldKeys = oldKeyValueDatabase.keys();
        for (int i = 0; i < DEFAULT_QUERY_NUM; i++) {
            long oldKey = oldKeys[secureRandom.nextInt(n)];
            List<byte[]> queryPayload = client.query(oldKey, DEFAULT_T, epsilon);
            Pair<FemurStatus, List<byte[]>> responsePayload = server.response(queryPayload);
            Pair<FemurStatus, byte[]> answer = client.retrieve(responsePayload);
            Assert.assertEquals(answer.getKey(), FemurStatus.HINT_V_MISMATCH);
            Assert.assertNull(answer.getValue());
        }
        // update PGM-index
        hintResponse = server.getHint();
        Assert.assertEquals(hintResponse.getLeft(), FemurStatus.SERVER_SUCC_RES);
        client.setHint(hintResponse.getRight());
        // query with new hint
        long[] newKeys = newKeyValueDatabase.keys();
        for (int i = 0; i < DEFAULT_QUERY_NUM; i++) {
            long newKey = newKeys[secureRandom.nextInt(n)];
            List<byte[]> queryPayload = client.query(newKey, DEFAULT_T, epsilon);
            Pair<FemurStatus, List<byte[]>> responsePayload = server.response(queryPayload);
            Pair<FemurStatus, byte[]> answer = client.retrieve(responsePayload);
            Assert.assertEquals(answer.getKey(), FemurStatus.SERVER_SUCC_RES);
            Assert.assertArrayEquals(answer.getValue(), newKeyValueDatabase.get(newKey));
        }
        server.reset();
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
