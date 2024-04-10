package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Zp-DOKVS test.
 *
 * @author Weiran Liu
 * @date 2024/2/19
 */
@RunWith(Parameterized.class)
public class ZpDokvsTest {
    /**
     * default prime
     */
    private static final BigInteger DEFAULT_PRIME = ZpManager.getPrime(CommonConstants.BLOCK_BIT_LENGTH * 2);
    /**
     * default Zp
     */
    private static final Zp DEFAULT_ZP = ZpFactory.createInstance(EnvType.STANDARD, DEFAULT_PRIME);
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default n
     */
    private static final int DEFAULT_N = 10;
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 10;
    /**
     * number of hashes
     */
    private final int hashKeyNum;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (ZpDokvsType type : ZpDokvsType.values()) {
            configurations.add(new Object[]{type.name(), type});
        }

        return configurations;
    }

    /**
     * type
     */
    private final ZpDokvsType type;

    public ZpDokvsTest(String name, ZpDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashKeyNum = ZpDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testIllegalInputs() {
        // try less keys
        if (hashKeyNum > 0) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(hashKeyNum - 1, SECURE_RANDOM);
                ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, DEFAULT_N, lessKeys);
            });
        }
        // try more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(hashKeyNum + 1, SECURE_RANDOM);
            ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, DEFAULT_N, moreKeys);
        });
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
        // try n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, 0, keys)
        );
        ZpDokvs<ByteBuffer> dokvs = ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, DEFAULT_N, keys);
        // try encode more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, BigInteger> keyValueMap = randomKeyValueMap(DEFAULT_ZP, DEFAULT_N + 1);
            dokvs.encode(keyValueMap, false);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, BigInteger> keyValueMap = randomKeyValueMap(DEFAULT_ZP, DEFAULT_N + 1);
            dokvs.encode(keyValueMap, true);
        });
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
        ZpDokvs<ByteBuffer> dokvs = ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, DEFAULT_N, keys);
        Assert.assertEquals(type, dokvs.getType());
    }

    @Test
    public void test1n() {
        testDokvs(1);
    }

    @Test
    public void test2n() {
        testDokvs(2);
    }

    @Test
    public void test3n() {
        testDokvs(3);
    }

    @Test
    public void test40n() {
        testDokvs(40);
    }

    @Test
    public void test256n() {
        testDokvs(256);
    }

    @Test
    public void test4096n() {
        testDokvs(4096);
    }

    private void testDokvs(int n) {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // create keys
            byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
            // create an instance
            ZpDokvs<ByteBuffer> dokvs = ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, n, keys);
            // generate key-value pairs
            Map<ByteBuffer, BigInteger> keyValueMap = randomKeyValueMap(DEFAULT_ZP, n);
            // non-doubly encode
            BigInteger[] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            Assert.assertEquals(ZpDokvsFactory.getM(type, n), nonDoublyStorage.length);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                BigInteger value = keyValueMap.get(key);
                BigInteger decodeValue = dokvs.decode(nonDoublyStorage, key);
                Assert.assertEquals(value, decodeValue);
            });
            // doubly encode
            BigInteger[] doublyStorage = dokvs.encode(keyValueMap, true);
            Assert.assertEquals(ZpDokvsFactory.getM(type, n), doublyStorage.length);
            // verify non-zero storage
            for (BigInteger x : doublyStorage) {
                Assert.assertNotEquals(BigInteger.ZERO, x);
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                BigInteger value = keyValueMap.get(key);
                BigInteger decodeValue = dokvs.decode(doublyStorage, key);
                Assert.assertEquals(value, decodeValue);
            });
            // verify randomly generate values are not in the set
            Set<BigInteger> valueSet = new HashSet<>(keyValueMap.values());
            IntStream.range(0, MAX_RANDOM_ROUND).forEach(index -> {
                byte[] randomKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(randomKeyBytes);
                ByteBuffer randomKey = ByteBuffer.wrap(randomKeyBytes);
                if (!keyValueMap.containsKey(randomKey)) {
                    BigInteger randomDecodeValue = dokvs.decode(doublyStorage, randomKey);
                    Assert.assertFalse(valueSet.contains(randomDecodeValue));
                }
            });
        }
    }

    static Map<ByteBuffer, BigInteger> randomKeyValueMap(Zp zp, int size) {
        Map<ByteBuffer, BigInteger> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(keyBytes);
            BigInteger value = zp.createNonZeroRandom(SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), value);
        });
        return keyValueMap;
    }
}
