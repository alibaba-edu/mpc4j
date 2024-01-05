package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GF2K-DOKVS tests.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
@RunWith(Parameterized.class)
public class Gf2kDokvsTest {
    /**
     * κ
     */
    private static final int KAPPA = CommonConstants.STATS_BIT_LENGTH;
    /**
     * κ in bytes
     */
    private static final int BYTE_KAPPA = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * default n
     */
    private static final int DEFAULT_N = 1 << 10 + 1;
    /**
     * round
     */
    private static final int ROUND = 10;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (Gf2kDokvsType type : Gf2kDokvsType.values()) {
            configurations.add(new Object[]{type.name(), type});
        }

        return configurations;
    }

    /**
     * GF2K-DOKVS type
     */
    private final Gf2kDokvsType type;
    /**
     * number of hashes
     */
    private final int hashNum;

    public Gf2kDokvsTest(String name, Gf2kDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = Gf2kDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testIllegalInputs() {
        // try setting more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(hashNum + 1, SECURE_RANDOM);
            Gf2kDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, moreKeys);
        });
        // try setting less keys
        if (Gf2kDokvsFactory.getHashKeyNum(type) > 0) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(hashNum - 1, SECURE_RANDOM);
                Gf2kDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, lessKeys);
            });
        }
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
        // try n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            Gf2kDokvsFactory.createInstance(EnvType.STANDARD, type, 0, keys)
        );
        Gf2kDokvs<ByteBuffer> dokvs = Gf2kDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, keys);
        // try encode more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(DEFAULT_N + 1);
            dokvs.encode(keyValueMap, false);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(DEFAULT_N + 1);
            dokvs.encode(keyValueMap, true);
        });
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
        Gf2kDokvs<ByteBuffer> dokvs = Gf2kDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, keys);
        Assert.assertEquals(type, dokvs.getType());
    }

    @Test
    public void testDefault() {
        testDokvs(DEFAULT_N);
    }

    @Test
    public void testParallelDefault() {
        testDokvs(DEFAULT_N, true);
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
    public void test8n() {
        testDokvs(8);
    }

    @Test
    public void test9n() {
        testDokvs(9);
    }

    @Test
    public void test40n() {
        testDokvs(40);
    }

    @Test
    public void testLog8n() {
        testDokvs(1 << 8);
    }

    @Test
    public void testLog10n() {
        testDokvs(1 << 10);
    }

    @Test
    public void testLog12n() {
        testDokvs(1 << 12);
    }

    @Test
    public void testLog16n() {
        testDokvs(1 << 14);
    }

    @Test
    public void testParallelLog16n() {
        testDokvs(1 << 14, true);
    }

    private void testDokvs(int n) {
        testDokvs(n, false);
    }

    private void testDokvs(int n, boolean parallelEncode) {
        for (int round = 0; round < ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            Gf2kDokvs<ByteBuffer> dokvs = Gf2kDokvsFactory.createInstance(EnvType.STANDARD, type, n, keys);
            dokvs.setParallelEncode(parallelEncode);
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(n);
            // non-doubly encode
            byte[][] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            Assert.assertEquals(Gf2kDokvsFactory.getM(EnvType.STANDARD, type, n), nonDoublyStorage.length);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                byte[] decodeValue = dokvs.decode(nonDoublyStorage, key);
                Assert.assertArrayEquals(value, decodeValue);
            });
            // doubly encode
            byte[][] doublyStorage = dokvs.encode(keyValueMap, true);
            Assert.assertEquals(Gf2kDokvsFactory.getM(EnvType.STANDARD, type, n), nonDoublyStorage.length);
            // verify non-zero storage
            byte[] zero = new byte[BYTE_KAPPA];
            Arrays.fill(zero, (byte) 0x00);
            for (byte[] x : doublyStorage) {
                Assert.assertFalse(Arrays.equals(x, zero));
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                byte[] decodeValue = dokvs.decode(doublyStorage, key);
                Assert.assertArrayEquals(value, decodeValue);
            });
            // random elements are not in the set
            Set<ByteBuffer> valueSet = keyValueMap.values().stream().map(ByteBuffer::wrap).collect(Collectors.toSet());
            IntStream.range(0, ROUND).forEach(index -> {
                // generate random key bytes
                byte[] randomKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(randomKeyBytes);
                ByteBuffer randomKey = ByteBuffer.wrap(randomKeyBytes);
                if (!keyValueMap.containsKey(randomKey)) {
                    byte[] randomDecodeValue = dokvs.decode(doublyStorage, randomKey);
                    Assert.assertFalse(valueSet.contains(ByteBuffer.wrap(randomDecodeValue)));
                }
            });
        }
    }

    static Map<ByteBuffer, byte[]> randomKeyValueMap(int n) {
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, n).forEach(index -> {
            byte[] keyBytes = new byte[BYTE_KAPPA];
            SECURE_RANDOM.nextBytes(keyBytes);
            byte[] valueBytes = BytesUtils.randomByteArray(BYTE_KAPPA, KAPPA, SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), valueBytes);
        });
        return keyValueMap;
    }
}
