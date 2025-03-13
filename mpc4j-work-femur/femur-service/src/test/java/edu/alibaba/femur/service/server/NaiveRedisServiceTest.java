package edu.alibaba.femur.service.server;

import com.google.common.base.Preconditions;
import edu.alibaba.femur.service.client.FemurPirClient;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;
import edu.alibaba.work.femur.demo.FemurStatus;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoRedisPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoRedisPirConfig;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class NaiveRedisServiceTest {
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 4;
    /**
     * default t
     */
    private static final int DEFAULT_T = 1 << 10;
    /**
     * host
     */
    private static final String HOST = "127.0.0.1";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Femur naive memory PIR
        configurations.add(new Object[]{
            FemurDemoPirType.NAIVE_MEMORY.name(), 6666, new NaiveFemurDemoMemoryPirConfig.Builder().build()
        });
        // Femur naive Redis PIR
        configurations.add(new Object[]{
            FemurDemoPirType.NAIVE_REDIS.name(), 6667, new NaiveFemurDemoRedisPirConfig.Builder().build()
        });
        // Femur SEAL memory PIR
        configurations.add(new Object[]{
            FemurDemoPirType.SEAL_MEMORY.name(), 6668, new SealFemurDemoMemoryPirConfig.Builder().build()
        });
        // Femur naive Redis PIR
        configurations.add(new Object[]{
            FemurDemoPirType.SEAL_REDIS.name(), 6669, new SealFemurDemoRedisPirConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * port
     */
    private final int port;
    /**
     * config
     */
    private final FemurDemoPirConfig config;
    /**
     * server
     */
    private final FemurPirServerBoot serverBoot;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public NaiveRedisServiceTest(String name, int port, FemurDemoPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.port = port;
        this.config = config;
        serverBoot = FemurPirServerBootFactory.getInstance(HOST, port, config);
        secureRandom = new SecureRandom();
    }

    @Before
    public void setUp() {
        serverBoot.start();
    }

    @After
    public void tearDown() {
        serverBoot.stop();
    }

    @Test
    public void testRegister() throws InterruptedException {
        int n = 1 << 10;
        int l = Long.SIZE;
        try {
            serverBoot.reset();
            FemurPirClient client = new FemurPirClient(HOST, port, "Alice", config);
            client.setUp();
            // register without init
            FemurStatus registerStatus = client.register();
            Assert.assertEquals(FemurStatus.SERVER_NOT_INIT, registerStatus);
            // register without setting database
            serverBoot.init(n, l);
            registerStatus = client.register();
            Assert.assertEquals(FemurStatus.SERVER_NOT_KVDB, registerStatus);
            // register with setting database
            TLongObjectMap<byte[]> database = generateKeyValueDatabase(n, l, secureRandom);
            serverBoot.setDatabase(database);
            registerStatus = client.register();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
            client.tearDown();
        } catch (StatusRuntimeException e) {
            Assert.fail("RPC failed: " + e.getStatus());
        }
    }

    @Test
    public void testHints() throws InterruptedException {
        int n = 1 << 10;
        int l = Long.SIZE;
        try {
            serverBoot.reset();
            FemurPirClient client = new FemurPirClient(HOST, port, "Alice", config);
            client.setUp();
            // hint without init
            FemurStatus hintStatus = client.getHint();
            Assert.assertEquals(FemurStatus.SERVER_NOT_INIT, hintStatus);
            // hint without setting database
            serverBoot.init(n, l);
            hintStatus = client.getHint();
            Assert.assertEquals(FemurStatus.SERVER_NOT_KVDB, hintStatus);
            // hint with setting database, client must register before getting hint
            TLongObjectMap<byte[]> database = generateKeyValueDatabase(n, l, secureRandom);
            serverBoot.setDatabase(database);
            FemurStatus registerStatus = client.register();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
            hintStatus = client.getHint();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, hintStatus);
            client.tearDown();
        } catch (StatusRuntimeException e) {
            Assert.fail("RPC failed: " + e.getStatus());
        }
    }

    @Test
    public void testQuery() throws InterruptedException {
        int n = 1 << 16;
        int l = Long.SIZE;
        try {
            serverBoot.reset();
            serverBoot.init(n, l);
            TLongObjectMap<byte[]> database = generateKeyValueDatabase(n, l, secureRandom);
            serverBoot.setDatabase(database);
            FemurPirClient client = new FemurPirClient(HOST, port, "Alice", config);
            client.setUp();
            // register and hint
            FemurStatus registerStatus = client.register();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
            FemurStatus hintStatus = client.getHint();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, hintStatus);
            // queries
            long[] keys = database.keys();
            for (int i = 0; i < DEFAULT_QUERY_NUM; i++) {
                long key = keys[secureRandom.nextInt(n)];
                Pair<FemurStatus, byte[]> response = client.query(key, DEFAULT_T, 0.01);
                Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, response.getLeft());
                Assert.assertArrayEquals(database.get(key), response.getRight());
            }
            client.tearDown();
        } catch (StatusRuntimeException e) {
            Assert.fail("RPC failed: " + e.getStatus());
        }
    }

    @Test
    public void testMultiClientQueries() throws InterruptedException {
        int n = 1 << 16;
        int l = Long.SIZE;
        final int numberOfClients = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        try {
            serverBoot.reset();
            serverBoot.init(n, l);
            TLongObjectMap<byte[]> database = generateKeyValueDatabase(n, l, secureRandom);
            serverBoot.setDatabase(database);
            // create multiple clients
            for (int i = 0; i < 10; i++) {
                int finalI = i;
                executor.submit(() -> {
                        try {
                            FemurPirClient eachClient = new FemurPirClient(HOST, port, "Party_" + finalI, config);
                            eachClient.setUp();
                            FemurStatus registerStatus = eachClient.register();
                            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
                            FemurStatus hintStatus = eachClient.getHint();
                            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, hintStatus);
                            // queries
                            long[] keys = database.keys();
                            for (int i1 = 0; i1 < DEFAULT_QUERY_NUM; i1++) {
                                long key = keys[secureRandom.nextInt(n)];
                                Pair<FemurStatus, byte[]> response = eachClient.query(key, DEFAULT_T, 0.01);
                                Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, response.getLeft());
                                Assert.assertArrayEquals(database.get(key), response.getRight());
                            }
                            eachClient.tearDown();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
            }
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (StatusRuntimeException e) {
            Assert.fail("RPC failed: " + e.getStatus());
        }
    }

    @Test
    public void testUpdateValue() throws InterruptedException {
        int n = 1 << 16;
        int l = Long.SIZE;
        int byteL = CommonUtils.getByteLength(l);
        final int numberOfClients = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        try {
            serverBoot.reset();
            serverBoot.init(n, l);
            TLongObjectMap<byte[]> database = generateKeyValueDatabase(n, l, secureRandom);
            serverBoot.setDatabase(database);
            // create multiple clients
            for (int i = 0; i < 10; i++) {
                int finalI = i;
                executor.submit(() -> {
                        try {
                            FemurPirClient eachClient = new FemurPirClient(HOST, port, "Party_" + finalI, config);
                            eachClient.setUp();
                            FemurStatus registerStatus = eachClient.register();
                            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
                            FemurStatus hintStatus = eachClient.getHint();
                            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, hintStatus);
                            // queries
                            long[] keys = database.keys();
                            for (int i1 = 0; i1 < DEFAULT_QUERY_NUM; i1++) {
                                long key = keys[secureRandom.nextInt(n)];
                                Pair<FemurStatus, byte[]> response = eachClient.query(key, DEFAULT_T, 0.01);
                                Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, response.getLeft());
                                Assert.assertArrayEquals(database.get(key), response.getRight());
                            }
                            eachClient.tearDown();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
            }
            long[] keys = database.keys();
            for (int j = 0; j < 10; j++) {
                long key = keys[secureRandom.nextInt(n)];
                serverBoot.updateValue(key, BytesUtils.randomByteArray(byteL, l, secureRandom));
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (StatusRuntimeException e) {
            Assert.fail("RPC failed: " + e.getStatus());
        }
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
