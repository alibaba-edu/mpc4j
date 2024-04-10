package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
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
 * GF(2^e)-DOKVS tests.
 *
 * @author Weiran Liu
 * @date 2022/01/06
 */
@RunWith(Parameterized.class)
public class Gf2eDokvsTest {
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
    /**
     * default l
     */
    private static final int DEFAULT_L = 128;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (Gf2eDokvsType type : Gf2eDokvsType.values()) {
            configurations.add(new Object[]{type.name(), type});
        }

        return configurations;
    }

    /**
     * GF(2^l)-DOKVS type
     */
    private final Gf2eDokvsType type;
    /**
     * number of hashes
     */
    private final int hashKeyNum;

    public Gf2eDokvsTest(String name, Gf2eDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashKeyNum = Gf2eDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testIllegalInputs() {
        // try setting more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(hashKeyNum + 1, SECURE_RANDOM);
            Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, DEFAULT_L, moreKeys);
        });
        // try setting less keys
        if (Gf2eDokvsFactory.getHashKeyNum(type) > 0) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(hashKeyNum - 1, SECURE_RANDOM);
                Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, DEFAULT_L, lessKeys);
            });
        }
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
        // try n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, 0, DEFAULT_L, keys)
        );
        Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, DEFAULT_L, keys);
        // try encode more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(DEFAULT_N + 1, DEFAULT_L);
            dokvs.encode(keyValueMap, false);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(DEFAULT_N + 1, DEFAULT_L);
            dokvs.encode(keyValueMap, true);
        });
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
        Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, DEFAULT_L, keys);
        Assert.assertEquals(type, dokvs.getType());
    }

    @Test
    public void testDefault() {
        testDokvs(DEFAULT_N);
    }

    @Test
    public void testParallelDefault() {
        testDokvs(DEFAULT_N, DEFAULT_L, true);
    }

    @Test
    public void testSpecialL() {
        testDokvs(DEFAULT_N, DEFAULT_L - 1);
        testDokvs(DEFAULT_N, DEFAULT_L + 1);
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
        // we need to test n > (1 << 14) for cluster version
        testDokvs(1 << 16);
    }

    @Test
    public void testParallelLog16n() {
        // we need to test n > (1 << 14) for cluster version
        testDokvs(1 << 16, DEFAULT_L, true);
    }

    private void testDokvs(int n) {
        testDokvs(n, DEFAULT_L);
    }

    private void testDokvs(int n, int l) {
        testDokvs(n, l, false);
    }

    private void testDokvs(int n, int l, boolean parallelEncode) {
        int byteL = CommonUtils.getByteLength(l);
        for (int round = 0; round < ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, SECURE_RANDOM);
            Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, n, l, keys);
            dokvs.setParallelEncode(parallelEncode);
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(n, l);
            // non-doubly encode
            byte[][] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            Assert.assertEquals(Gf2eDokvsFactory.getM(EnvType.STANDARD, type, n), nonDoublyStorage.length);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                byte[] decodeValue = dokvs.decode(nonDoublyStorage, key);
                Assert.assertArrayEquals(value, decodeValue);
            });
            // doubly encode
            byte[][] doublyStorage = dokvs.encode(keyValueMap, true);
            Assert.assertEquals(Gf2eDokvsFactory.getM(EnvType.STANDARD, type, n), nonDoublyStorage.length);
            // verify non-zero storage
            byte[] zero = new byte[byteL];
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

    static Map<ByteBuffer, byte[]> randomKeyValueMap(int n, int l) {
        int byteL = CommonUtils.getByteLength(l);
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, n).forEach(index -> {
            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(keyBytes);
            byte[] valueBytes = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), valueBytes);
        });
        return keyValueMap;
    }
}
