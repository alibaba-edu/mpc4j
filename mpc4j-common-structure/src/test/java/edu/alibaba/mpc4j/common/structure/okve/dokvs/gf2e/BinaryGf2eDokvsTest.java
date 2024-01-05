package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import com.google.common.base.Preconditions;
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

/**
 * binary GF(2^e)-DOKVS tests.
 *
 * @author Weiran Liu
 * @date 2022/01/06
 */
@RunWith(Parameterized.class)
public class BinaryGf2eDokvsTest {
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
            if (Gf2eDokvsFactory.isBinary(type)) {
                configurations.add(new Object[]{type.name(), type});
            }
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
    private final int hashNum;

    public BinaryGf2eDokvsTest(String name, Gf2eDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = Gf2eDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testDefault() {
        testBinaryDokvs(DEFAULT_N);
    }

    @Test
    public void testSpecialL() {
        testBinaryDokvs(DEFAULT_N, DEFAULT_L - 1);
        testBinaryDokvs(DEFAULT_N, DEFAULT_L + 1);
    }

    @Test
    public void test1n() {
        testBinaryDokvs(1);
    }

    @Test
    public void test2n() {
        testBinaryDokvs(2);
    }

    @Test
    public void test3n() {
        testBinaryDokvs(3);
    }

    @Test
    public void test40n() {
        testBinaryDokvs(40);
    }

    @Test
    public void testLog8n() {
        testBinaryDokvs(1 << 8);
    }

    @Test
    public void testLog10n() {
        testBinaryDokvs(1 << 10);
    }

    @Test
    public void testLog12n() {
        testBinaryDokvs(1 << 12);
    }

    @Test
    public void testLog14n() {
        testBinaryDokvs(1 << 14);
    }

    private void testBinaryDokvs(int n) {
        testBinaryDokvs(n, DEFAULT_L);
    }

    private void testBinaryDokvs(int n, int l) {
        int byteL = CommonUtils.getByteLength(l);
        for (int round = 0; round < ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            BinaryGf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createBinaryInstance(EnvType.STANDARD, type, n, l, keys);
            Map<ByteBuffer, byte[]> keyValueMap = Gf2eDokvsTest.randomKeyValueMap(n, l);
            // non-doubly encode
            byte[][] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                int[] positions = dokvs.positions(key);
                byte[] decodeValue = BytesUtils.innerProduct(nonDoublyStorage, byteL, positions);
                Assert.assertArrayEquals(value, decodeValue);
            });
            // doubly encode
            byte[][] doublyStorage = dokvs.encode(keyValueMap, true);
            // verify non-zero storage
            byte[] zero = new byte[byteL];
            Arrays.fill(zero, (byte) 0x00);
            for (byte[] x : doublyStorage) {
                Assert.assertFalse(Arrays.equals(x, zero));
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                int[] positions = dokvs.positions(key);
                byte[] decodeValue = BytesUtils.innerProduct(doublyStorage, byteL, positions);
                Assert.assertArrayEquals(value, decodeValue);
            });
        }
    }
}
