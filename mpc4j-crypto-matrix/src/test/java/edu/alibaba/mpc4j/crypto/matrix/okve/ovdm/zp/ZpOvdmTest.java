package edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Zp-OVDM test.
 *
 * @author Weiran Liu
 * @date 2022/01/09
 */
@RunWith(Parameterized.class)
public class ZpOvdmTest {
    /**
     * default n
     */
    private static final int DEFAULT_N = 10;
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{ZpOvdmType.LPRST21_GBF.name(), ZpOvdmType.LPRST21_GBF});
        // H2_TWO_CORE_GCT
        configurations.add(new Object[]{ZpOvdmType.H2_TWO_CORE_GCT.name(), ZpOvdmType.H2_TWO_CORE_GCT});
        // H2_SINGLETON_GCT
        configurations.add(new Object[]{ZpOvdmType.H2_SINGLETON_GCT.name(), ZpOvdmType.H2_SINGLETON_GCT});
        // H3_SINGLETON_GCT
        configurations.add(new Object[]{ZpOvdmType.H3_SINGLETON_GCT.name(), ZpOvdmType.H3_SINGLETON_GCT});

        return configurations;
    }

    /**
     * type
     */
    private final ZpOvdmType type;

    public ZpOvdmTest(String name, ZpOvdmType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // try less keys
        if (ZpOvdmFactory.getHashNum(type) > 0) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(
                    ZpOvdmFactory.getHashNum(type) - 1, ZpOvdmTestUtils.SECURE_RANDOM
                );
                ZpOvdmFactory.createInstance(
                    EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, DEFAULT_N, lessKeys
                );
            });
        }
        // try more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(
                ZpOvdmFactory.getHashNum(type) + 1, ZpOvdmTestUtils.SECURE_RANDOM
            );
            ZpOvdmFactory.createInstance(
                EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, DEFAULT_N, moreKeys
            );
        });
        byte[][] keys = CommonUtils.generateRandomKeys(ZpOvdmFactory.getHashNum(type), ZpOvdmTestUtils.SECURE_RANDOM);
        // try n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            ZpOvdmFactory.createInstance(EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, 0, keys)
        );
        ZpOvdm<ByteBuffer> ovdm = ZpOvdmFactory.createInstance(
            EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, DEFAULT_N, keys
        );
        // try encode more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Map<ByteBuffer, BigInteger> keyValueMap = ZpOvdmTestUtils.randomKeyValueMap(DEFAULT_N + 1);
            IntStream.range(0, DEFAULT_N + 1).forEach(index -> {
                byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                ZpOvdmTestUtils.SECURE_RANDOM.nextBytes(keyBytes);
                BigInteger value = ZpOvdmTestUtils.DEFAULT_ZP.createNonZeroRandom(ZpOvdmTestUtils.SECURE_RANDOM);
                keyValueMap.put(ByteBuffer.wrap(keyBytes), value);
                ovdm.encode(keyValueMap);
            });
        });
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(ZpOvdmFactory.getHashNum(type), ZpOvdmTestUtils.SECURE_RANDOM);
        ZpOvdm<ByteBuffer> ovdm = ZpOvdmFactory.createInstance(
            EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, DEFAULT_N, keys
        );
        Assert.assertEquals(type, ovdm.getZpOvdmType());
    }

    @Test
    public void test1n() {
        testOvdm(1);
    }

    @Test
    public void test2n() {
        testOvdm(2);
    }

    @Test
    public void test3n() {
        testOvdm(3);
    }

    @Test
    public void test40n() {
        testOvdm(40);
    }

    @Test
    public void test256n() {
        testOvdm(256);
    }

    @Test
    public void test4096n() {
        testOvdm(4096);
    }

    private void testOvdm(int n) {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // create keys
            byte[][] keys = CommonUtils.generateRandomKeys(
                ZpOvdmFactory.getHashNum(type), ZpOvdmTestUtils.SECURE_RANDOM
            );
            // create an instance
            ZpOvdm<ByteBuffer> odvm = ZpOvdmFactory.createInstance(
                EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, n, keys
            );
            // generate key-value pairs
            Map<ByteBuffer, BigInteger> keyValueMap = ZpOvdmTestUtils.randomKeyValueMap(n);
            // encode
            BigInteger[] storage = odvm.encode(keyValueMap);
            // parallel decode
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                BigInteger value = keyValueMap.get(key);
                BigInteger decodeValue = odvm.decode(storage, key);
                Assert.assertEquals(value, decodeValue);
            });
            // verify randomly generate values are not in the set
            Set<BigInteger> valueSet = new HashSet<>(keyValueMap.values());
            IntStream.range(0, MAX_RANDOM_ROUND).forEach(index -> {
                // 生成比特长度为安全常数的x，生成l比特长的y，插入到Map中
                byte[] randomKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                ZpOvdmTestUtils.SECURE_RANDOM.nextBytes(randomKeyBytes);
                ByteBuffer randomKey = ByteBuffer.wrap(randomKeyBytes);
                if (!keyValueMap.containsKey(randomKey)) {
                    BigInteger randomDecodeValue = odvm.decode(storage, randomKey);
                    Assert.assertFalse(valueSet.contains(randomDecodeValue));
                }
            });
        }
    }
}
