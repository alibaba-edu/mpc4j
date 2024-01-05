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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * sparse GF(2^e)-DOKVS tests.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
@RunWith(Parameterized.class)
public class SparseGf2eDokvsTest {
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
            if (Gf2eDokvsFactory.isSparse(type)) {
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

    public SparseGf2eDokvsTest(String name, Gf2eDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = Gf2eDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testDefault() {
        testSparseDokvs(DEFAULT_N);
    }

    @Test
    public void testSpecialL() {
        testSparseDokvs(DEFAULT_N, DEFAULT_L - 1);
        testSparseDokvs(DEFAULT_N, DEFAULT_L + 1);
    }

    @Test
    public void test1n() {
        testSparseDokvs(1);
    }

    @Test
    public void test2n() {
        testSparseDokvs(2);
    }

    @Test
    public void test3n() {
        testSparseDokvs(3);
    }

    @Test
    public void test40n() {
        testSparseDokvs(40);
    }

    @Test
    public void testLog8n() {
        testSparseDokvs(1 << 8);
    }

    @Test
    public void testLog10n() {
        testSparseDokvs(1 << 10);
    }

    @Test
    public void testLog12n() {
        testSparseDokvs(1 << 12);
    }

    @Test
    public void testLog14n() {
        testSparseDokvs(1 << 14);
    }

    private void testSparseDokvs(int n) {
        testSparseDokvs(n, DEFAULT_L);
    }

    private void testSparseDokvs(int n, int l) {
        int byteL = CommonUtils.getByteLength(l);
        for (int round = 0; round < ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            SparseGf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createSparseInstance(EnvType.STANDARD, type, n, l, keys);
            Map<ByteBuffer, byte[]> keyValueMap = Gf2eDokvsTest.randomKeyValueMap(n, l);
            int sparseRange = dokvs.sparsePositionRange();
            int denseRange = dokvs.densePositionRange();
            // non-doubly encode
            byte[][] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            byte[][] nonDoublySparseStorage = new byte[sparseRange][];
            System.arraycopy(nonDoublyStorage, 0, nonDoublySparseStorage, 0, sparseRange);
            byte[][] nonDoublyDenseStorage = new byte[denseRange][];
            System.arraycopy(nonDoublyStorage, sparseRange, nonDoublyDenseStorage, 0, denseRange);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                int[] sparsePositions = dokvs.sparsePositions(key);
                boolean[] densePositions = dokvs.binaryDensePositions(key);
                byte[] decodeValue = BytesUtils.innerProduct(nonDoublySparseStorage, byteL, sparsePositions);
                for (int densePosition = 0; densePosition < denseRange; densePosition++) {
                    if (densePositions[densePosition]) {
                        BytesUtils.xori(decodeValue, nonDoublyDenseStorage[densePosition]);
                    }
                }
                Assert.assertArrayEquals(value, decodeValue);
            });
            // doubly encode
            byte[][] doublyStorage = dokvs.encode(keyValueMap, true);
            byte[][] doublySparseStorage = new byte[sparseRange][];
            System.arraycopy(doublyStorage, 0, doublySparseStorage, 0, sparseRange);
            byte[][] doublyDenseStorage = new byte[denseRange][];
            System.arraycopy(doublyStorage, sparseRange, doublyDenseStorage, 0, denseRange);
            // verify non-zero storage
            byte[] zero = new byte[byteL];
            Arrays.fill(zero, (byte) 0x00);
            for (byte[] x : doublyStorage) {
                Assert.assertFalse(Arrays.equals(x, zero));
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                int[] sparsePositions = dokvs.sparsePositions(key);
                boolean[] densePositions = dokvs.binaryDensePositions(key);
                byte[] decodeValue = BytesUtils.innerProduct(doublySparseStorage, byteL, sparsePositions);
                for (int densePosition = 0; densePosition < denseRange; densePosition++) {
                    if (densePositions[densePosition]) {
                        BytesUtils.xori(decodeValue, doublyDenseStorage[densePosition]);
                    }
                }
                Assert.assertArrayEquals(value, decodeValue);
            });
        }
    }
}
