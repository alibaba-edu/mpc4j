package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * ECC-DOKVS test.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
@RunWith(Parameterized.class)
public class EccDokvsTest {
    /**
     * default ECC
     */
    private static final Ecc DEFAULT_ECC = EccFactory.createInstance(EnvType.STANDARD);
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
    private static final int MAX_RANDOM_ROUND = 3;
    /**
     * number of hashes
     */
    private final int hashKeyNum;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (EccDokvsType type : EccDokvsType.values()) {
            configurations.add(new Object[]{type.name(), type});
        }

        return configurations;
    }

    /**
     * type
     */
    private final EccDokvsType type;

    public EccDokvsTest(String name, EccDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashKeyNum = EccDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testIllegalInputs() {
        // try less keys
        if (hashKeyNum > 0) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(hashKeyNum - 1, SECURE_RANDOM);
                EccDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_ECC, DEFAULT_N, lessKeys);
            });
        }
        // try more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(hashKeyNum + 1, SECURE_RANDOM);
            EccDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_ECC, DEFAULT_N, moreKeys);
        });
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
        // try n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            EccDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_ECC, 0, keys)
        );
        EccDokvs<ByteBuffer> dokvs = EccDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_ECC, DEFAULT_N, keys);
        // try encode more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, ECPoint> keyValueMap = randomKeyValueMap(DEFAULT_ECC, DEFAULT_N + 1);
            dokvs.encode(keyValueMap, false);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, ECPoint> keyValueMap = randomKeyValueMap(DEFAULT_ECC, DEFAULT_N + 1);
            dokvs.encode(keyValueMap, true);
        });
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
        EccDokvs<ByteBuffer> dokvs = EccDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_ECC, DEFAULT_N, keys);
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
    public void test1024n() {
        testDokvs(1024);
    }

    private void testDokvs(int n) {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // create keys
            byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
            // create an instance
            EccDokvs<ByteBuffer> dokvs = EccDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_ECC, n, keys);
            // generate key-value pairs
            Map<ByteBuffer, ECPoint> keyValueMap = randomKeyValueMap(DEFAULT_ECC, n);
            // non-doubly encode
            ECPoint[] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            Assert.assertEquals(EccDokvsFactory.getM(type, n), nonDoublyStorage.length);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                ECPoint value = keyValueMap.get(key);
                ECPoint decodeValue = dokvs.decode(nonDoublyStorage, key);
                Assert.assertEquals(value, decodeValue);
            });
            // doubly encode
            ECPoint[] doublyStorage = dokvs.encode(keyValueMap, true);
            Assert.assertEquals(EccDokvsFactory.getM(type, n), doublyStorage.length);
            // verify non-zero storage
            for (ECPoint x : doublyStorage) {
                Assert.assertNotEquals(DEFAULT_ECC.getInfinity(), x);
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                ECPoint value = keyValueMap.get(key);
                ECPoint decodeValue = dokvs.decode(doublyStorage, key);
                Assert.assertEquals(value, decodeValue);
            });
            // verify randomly generate values are not in the set
            Set<ECPoint> valueSet = new HashSet<>(keyValueMap.values());
            IntStream.range(0, MAX_RANDOM_ROUND).forEach(index -> {
                byte[] randomKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(randomKeyBytes);
                ByteBuffer randomKey = ByteBuffer.wrap(randomKeyBytes);
                if (!keyValueMap.containsKey(randomKey)) {
                    ECPoint randomDecodeValue = dokvs.decode(doublyStorage, randomKey);
                    Assert.assertFalse(valueSet.contains(randomDecodeValue));
                }
            });
        }
    }

    static Map<ByteBuffer, ECPoint> randomKeyValueMap(Ecc ecc, int size) {
        Map<ByteBuffer, ECPoint> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(keyBytes);
            org.bouncycastle.math.ec.ECPoint value = ecc.randomPoint(SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), value);
        });
        return keyValueMap;
    }
}
