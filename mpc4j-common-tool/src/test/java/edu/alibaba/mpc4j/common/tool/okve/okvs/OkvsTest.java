package edu.alibaba.mpc4j.common.tool.okve.okvs;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * OKVS tests.
 *
 * @author Weiran Liu
 * @date 2022/01/02
 */
@RunWith(Parameterized.class)
public class OkvsTest {
    /**
     * default L
     */
    private static final int DEFAULT_L = 64;
    /**
     * the random state
     */
    static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default n
     */
    private static final int DEFAULT_N = 10;
    /**
     * random test round
     */
    private static final int MAX_RANDOM_ROUND = 50;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // H3_SINGLETON_GCT
        configurations.add(new Object[]{OkvsType.H3_SINGLETON_GCT.name(), OkvsType.H3_SINGLETON_GCT});
        // H2_SINGLETON_GCT
        configurations.add(new Object[]{OkvsType.H2_SINGLETON_GCT.name(), OkvsType.H2_SINGLETON_GCT});
        // H2_TWO_CORE_GCT
        configurations.add(new Object[]{OkvsType.H2_TWO_CORE_GCT.name(), OkvsType.H2_TWO_CORE_GCT});
        // H2_DFS_GCT
        configurations.add(new Object[]{OkvsType.H2_DFS_GCT.name(), OkvsType.H2_DFS_GCT});
        // GBF
        configurations.add(new Object[]{OkvsType.GBF.name(), OkvsType.GBF});
        // MEGA_BIN
        configurations.add(new Object[]{OkvsType.MEGA_BIN.name(), OkvsType.MEGA_BIN});
        // POLYNOMIAL
        configurations.add(new Object[]{OkvsType.POLYNOMIAL.name(), OkvsType.POLYNOMIAL});

        return configurations;
    }

    /**
     * OKVS type
     */
    private final OkvsType type;
    /**
     * hash num
     */
    private final int hashNum;

    public OkvsTest(String name, OkvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = OkvsFactory.getHashNum(type);
    }

    @Test
    public void testIllegalInputs() {
        int l = DEFAULT_L;
        int n = DEFAULT_N;
        if (OkvsFactory.getHashNum(type) > 0) {
            // creates OKVS with more keys
            Assert.assertThrows(AssertionError.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(hashNum + 1, SECURE_RANDOM);
                OkvsFactory.createInstance(EnvType.STANDARD, type, n, l, lessKeys);
            });
            // creates OKVS with fewer keys
            Assert.assertThrows(AssertionError.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(hashNum - 1, SECURE_RANDOM);
                OkvsFactory.createInstance(EnvType.STANDARD, type, n, l, lessKeys);
            });
        }
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
        // creates OKVS with l < σ
        Assert.assertThrows(AssertionError.class, () ->
            OkvsFactory.createInstance(EnvType.STANDARD, type, n, CommonConstants.STATS_BIT_LENGTH - 1, keys)
        );
        // creates OKVS with n = 0
        Assert.assertThrows(AssertionError.class, () ->
            OkvsFactory.createInstance(EnvType.STANDARD, type, 0, l, keys)
        );
        // encodes more elements
        Assert.assertThrows(AssertionError.class, () -> {
            Map<String, byte[]> keyValueMap = randomKeyValueMap(n + 1, l);
            Okvs<String> okvs = OkvsFactory.createInstance(EnvType.STANDARD, type, n, l, keys);
            okvs.encode(keyValueMap);
        });
        Map<String, byte[]> keyValueMap = randomKeyValueMap(n, l);
        Okvs<String> okvs = OkvsFactory.createInstance(EnvType.STANDARD, type, n, l, keys);
        byte[][] storage = okvs.encode(keyValueMap);
        // decodes with small storage
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] smallStorage = Arrays.copyOf(storage, storage.length - 1);
            keyValueMap.keySet().forEach(key -> okvs.decode(smallStorage, key));
        });
        // decodes with large OKVS
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] largeStorage = Arrays.copyOf(storage, storage.length + 1);
            keyValueMap.keySet().forEach(key -> okvs.decode(largeStorage, key));
        });
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, DEFAULT_L, keys);
        Assert.assertEquals(type, okvs.getOkvsType());
    }

    @Test
    public void testEmptyOkvs() {
        int l = DEFAULT_L;
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
        Okvs<String> emptyOkvs = OkvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, l, keys);
        // creates an empty key-value map
        Map<String, byte[]> emptyKeyValueMap = randomKeyValueMap(0, l);
        byte[][] storage = emptyOkvs.encode(emptyKeyValueMap);
        Assert.assertEquals(emptyOkvs.getM(), storage.length);
        Arrays.stream(storage).forEach(row -> Assert.assertEquals(emptyOkvs.getL(), row.length * Byte.SIZE));
    }

    @Test
    public void test1n() {
        testOkvs(1, DEFAULT_L);
    }

    @Test
    public void test2n() {
        testOkvs(2, DEFAULT_L);
    }

    @Test
    public void test3n() {
        testOkvs(3, DEFAULT_L);
    }

    @Test
    public void test40n() {
        testOkvs(40, DEFAULT_L);
    }

    @Test
    public void test256n() {
        testOkvs(256, DEFAULT_L);
    }

    @Test
    public void test4096n() {
        // polynomial OKVS is very slow, ignore it
        if (!type.equals(OkvsType.POLYNOMIAL)) {
            testOkvs(4096, DEFAULT_L);
        }
    }

    @Test
    public void testSpecialL() {
        testOkvs(DEFAULT_N, DEFAULT_L + 5);
    }

    @Test
    public void testLargeL() {
        testOkvs(DEFAULT_N, DEFAULT_L * 2);
    }

    private void testOkvs(int n, int l) {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            Okvs<String> okvs = OkvsFactory.createInstance(EnvType.STANDARD, type, n, l, keys);
            Map<String, byte[]> keyValueMap = randomKeyValueMap(n, l);
            byte[][] storage = okvs.encode(keyValueMap);
            keyValueMap.keySet().stream()
                .parallel()
                .forEach(key -> {
                    byte[] valueBytes = keyValueMap.get(key);
                    byte[] decodeValueBytes = okvs.decode(storage, key);
                    Assert.assertArrayEquals(valueBytes, decodeValueBytes);
                });
        }
    }

    private Map<String, byte[]> randomKeyValueMap(int size, int l) {
        int byteL = CommonUtils.getByteLength(l);
        Map<String, byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            String key = RandomStringUtils.randomAlphanumeric(l);
            byte[] valueBytes = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
            keyValueMap.put(key, valueBytes);
        });
        return keyValueMap;
    }
}
