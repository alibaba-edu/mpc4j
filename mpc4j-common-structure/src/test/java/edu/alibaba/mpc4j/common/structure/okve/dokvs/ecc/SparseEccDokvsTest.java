package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * sparse ECC-DOKVS tests.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
@RunWith(Parameterized.class)
public class SparseEccDokvsTest {
    /**
     * default ECC
     */
    private static final Ecc DEFAULT_ECC = EccFactory.createInstance(EnvType.STANDARD);
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

        for (EccDokvsType type : EccDokvsType.values()) {
            if (EccDokvsFactory.isSparse(type)) {
                configurations.add(new Object[]{type.name(), type});
            }
        }

        return configurations;
    }

    /**
     * ECC-DOKVS type
     */
    private final EccDokvsType type;
    /**
     * number of hashes
     */
    private final int hashNum;

    public SparseEccDokvsTest(String name, EccDokvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = EccDokvsFactory.getHashKeyNum(type);
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

    private void testSparseDokvs(int n) {
        for (int round = 0; round < ROUND; round++) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            SparseEccDokvs<ByteBuffer> dokvs = EccDokvsFactory.createSparseInstance(EnvType.STANDARD, type, DEFAULT_ECC, n, keys);
            Map<ByteBuffer, ECPoint> keyValueMap = EccDokvsTest.randomKeyValueMap(DEFAULT_ECC, n);
            int sparseRange = dokvs.sparsePositionRange();
            int denseRange = dokvs.densePositionRange();
            // non-doubly encode
            ECPoint[] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            ECPoint[] nonDoublySparseStorage = new ECPoint[sparseRange];
            System.arraycopy(nonDoublyStorage, 0, nonDoublySparseStorage, 0, sparseRange);
            ECPoint[] nonDoublyDenseStorage = new ECPoint[denseRange];
            System.arraycopy(nonDoublyStorage, sparseRange, nonDoublyDenseStorage, 0, denseRange);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                ECPoint value = keyValueMap.get(key);
                int[] sparsePositions = dokvs.sparsePositions(key);
                boolean[] densePositions = dokvs.binaryDensePositions(key);
                ECPoint decodeValue = DEFAULT_ECC.getInfinity();
                for (int sparsePosition : sparsePositions) {
                    decodeValue = DEFAULT_ECC.add(decodeValue, nonDoublySparseStorage[sparsePosition]);
                }
                for (int densePosition = 0; densePosition < denseRange; densePosition++) {
                    if (densePositions[densePosition]) {
                        decodeValue = DEFAULT_ECC.add(decodeValue, nonDoublyDenseStorage[densePosition]);
                    }
                }
                Assert.assertEquals(value, decodeValue);
            });
            // doubly encode
            ECPoint[] doublyStorage = dokvs.encode(keyValueMap, true);
            ECPoint[] doublySparseStorage = new ECPoint[sparseRange];
            System.arraycopy(doublyStorage, 0, doublySparseStorage, 0, sparseRange);
            ECPoint[] doublyDenseStorage = new ECPoint[denseRange];
            System.arraycopy(doublyStorage, sparseRange, doublyDenseStorage, 0, denseRange);
            // verify non-zero storage
            for (ECPoint x : doublyStorage) {
                Assert.assertFalse(x.isInfinity());
            }
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                ECPoint value = keyValueMap.get(key);
                int[] sparsePositions = dokvs.sparsePositions(key);
                boolean[] densePositions = dokvs.binaryDensePositions(key);
                ECPoint decodeValue = DEFAULT_ECC.getInfinity();
                for (int sparsePosition : sparsePositions) {
                    decodeValue = DEFAULT_ECC.add(decodeValue, doublySparseStorage[sparsePosition]);
                }
                for (int densePosition = 0; densePosition < denseRange; densePosition++) {
                    if (densePositions[densePosition]) {
                        decodeValue = DEFAULT_ECC.add(decodeValue, doublyDenseStorage[densePosition]);
                    }
                }
                Assert.assertEquals(value, decodeValue);
            });
        }
    }
}
