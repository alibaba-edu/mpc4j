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

/**
 * sparse Zp-DOKVS tests.
 *
 * @author Weiran Liu
 * @date 2024/2/20
 */
@RunWith(Parameterized.class)
public class SparseZpDokvsTest {
    /**
     * default prime
     */
    private static final BigInteger DEFAULT_PRIME = ZpManager.getPrime(CommonConstants.BLOCK_BIT_LENGTH * 2);
    /**
     * default Zp
     */
    private static final Zp DEFAULT_ZP = ZpFactory.createInstance(EnvType.STANDARD, DEFAULT_PRIME);
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

        for (ZpDokvsType type : ZpDokvsType.values()) {
            if (ZpDokvsFactory.isSparse(type)) {
                configurations.add(new Object[]{type.name(), type});
            }
        }

        return configurations;
    }

    /**
     * ECC-DOKVS type
     */
    private final ZpDokvsType type;
    /**
     * number of hashes
     */
    private final int hashNum;

    public SparseZpDokvsTest(String name, ZpDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = ZpDokvsFactory.getHashKeyNum(type);
    }

    @Test
    public void testDefault() {
        testSparseDokvs((1 << 10) + 1);
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
        for (int round = 0; round < ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            SparseZpDokvs<ByteBuffer> dokvs = ZpDokvsFactory.createSparseInstance(EnvType.STANDARD, type, DEFAULT_PRIME, n, keys);
            Map<ByteBuffer, BigInteger> keyValueMap = ZpDokvsTest.randomKeyValueMap(DEFAULT_ZP, n);
            int sparseRange = dokvs.sparsePositionRange();
            int denseRange = dokvs.densePositionRange();
            // non-doubly encode
            BigInteger[] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            BigInteger[] nonDoublySparseStorage = new BigInteger[sparseRange];
            System.arraycopy(nonDoublyStorage, 0, nonDoublySparseStorage, 0, sparseRange);
            BigInteger[] nonDoublyDenseStorage = new BigInteger[denseRange];
            System.arraycopy(nonDoublyStorage, sparseRange, nonDoublyDenseStorage, 0, denseRange);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                BigInteger value = keyValueMap.get(key);
                int[] sparsePositions = dokvs.sparsePositions(key);
                boolean[] densePositions = dokvs.binaryDensePositions(key);
                BigInteger decodeValue = DEFAULT_ZP.createZero();
                for (int sparsePosition : sparsePositions) {
                    decodeValue = DEFAULT_ZP.add(decodeValue, nonDoublySparseStorage[sparsePosition]);
                }
                for (int densePosition = 0; densePosition < denseRange; densePosition++) {
                    if (densePositions[densePosition]) {
                        decodeValue = DEFAULT_ZP.add(decodeValue, nonDoublyDenseStorage[densePosition]);
                    }
                }
                Assert.assertEquals(value, decodeValue);
            });
            // doubly encode
            BigInteger[] doublyStorage = dokvs.encode(keyValueMap, true);
            BigInteger[] doublySparseStorage = new BigInteger[sparseRange];
            System.arraycopy(doublyStorage, 0, doublySparseStorage, 0, sparseRange);
            BigInteger[] doublyDenseStorage = new BigInteger[denseRange];
            System.arraycopy(doublyStorage, sparseRange, doublyDenseStorage, 0, denseRange);
            // verify non-zero storage
            for (BigInteger x : doublyStorage) {
                Assert.assertFalse(DEFAULT_ZP.isZero(x));
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                BigInteger value = keyValueMap.get(key);
                int[] sparsePositions = dokvs.sparsePositions(key);
                boolean[] densePositions = dokvs.binaryDensePositions(key);
                BigInteger decodeValue = DEFAULT_ZP.createZero();
                for (int sparsePosition : sparsePositions) {
                    decodeValue = DEFAULT_ZP.add(decodeValue, doublySparseStorage[sparsePosition]);
                }
                for (int densePosition = 0; densePosition < denseRange; densePosition++) {
                    if (densePositions[densePosition]) {
                        decodeValue = DEFAULT_ZP.add(decodeValue, doublyDenseStorage[densePosition]);
                    }
                }
                Assert.assertEquals(value, decodeValue);
            });
        }
    }
}
