package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Zl64 test.
 *
 * @author Weiran Liu
 * @date 2024/5/28
 */
@RunWith(Parameterized.class)
public class Zl64SpecialTest {
    /**
     * parallel num
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * random test num
     */
    private static final int MAX_RANDOM = 400;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            "l = " + 63 + ")", Zl64Factory.createInstance(EnvType.STANDARD, 63),
        });
        configurations.add(new Object[]{
            "l = " + 64 + ")", Zl64Factory.createInstance(EnvType.STANDARD, 64),
        });

        return configurations;
    }

    /**
     * Zl64 instance
     */
    private final Zl64 zl64;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Zl64SpecialTest(String name, Zl64 zl64) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.zl64 = zl64;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testCreateZero() {
        long zero = zl64.createZero();
        Assert.assertTrue(zl64.isZero(zero));
        Assert.assertFalse(zl64.isOne(zero));
    }

    @Test
    public void testCreateOne() {
        long one = zl64.createOne();
        Assert.assertTrue(zl64.isOne(one));
        Assert.assertFalse(zl64.isZero(one));
    }

    @Test
    public void testCreateRandom() {
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        // create random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            long randomElement = zl64.createRandom(secureRandom);
            Assert.assertTrue(zl64.validateElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                long randomElement = zl64.createRandom(seed);
                Assert.assertTrue(zl64.validateElement(randomElement));
                return randomElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testCreateNonZeroRandom() {
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        // create non-zero random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            long randomNonZeroElement = zl64.createNonZeroRandom(secureRandom);
            Assert.assertTrue(zl64.validateElement(randomNonZeroElement));
            Assert.assertTrue(zl64.validateNonZeroElement(randomNonZeroElement));
            Assert.assertFalse(zl64.isZero(randomNonZeroElement));
        });
        // create non-zero random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                long randomNonZeroElement = zl64.createNonZeroRandom(seed);
                Assert.assertTrue(zl64.validateElement(randomNonZeroElement));
                Assert.assertTrue(zl64.validateNonZeroElement(randomNonZeroElement));
                Assert.assertFalse(zl64.isZero(randomNonZeroElement));
                return randomNonZeroElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testCreateRangeRandom() {
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        // create range random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            long randomElement = zl64.createRangeRandom(secureRandom);
            Assert.assertTrue(zl64.validateElement(randomElement));
            Assert.assertTrue(zl64.validateRangeElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                long randomRangeElement = zl64.createRangeRandom(seed);
                Assert.assertTrue(zl64.validateElement(randomRangeElement));
                Assert.assertTrue(zl64.validateRangeElement(randomRangeElement));
                return randomRangeElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testConstantAddNegSub() {
        long zero = zl64.createZero();
        long p;
        long t;
        // 0 + 0 = 0
        p = zl64.createZero();
        t = zl64.add(p, zero);
        Assert.assertEquals(zero, t);
        // -0 = 0
        p = zl64.createZero();
        t = zl64.neg(p);
        Assert.assertEquals(zero, t);
        // 0 - 0 = 0
        p = zl64.createZero();
        t = zl64.sub(p, zero);
        Assert.assertEquals(zero, t);
    }

    @Test
    public void testRandomAddNegSub() {
        long zero = zl64.createZero();
        long r;
        long s;
        long t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            r = zl64.createRandom(secureRandom);
            s = zl64.createRandom(secureRandom);
            // r + 0 = r
            t = zl64.add(r, zero);
            Assert.assertEquals(r, t);
            // r - 0 = r
            t = zl64.sub(r, zero);
            Assert.assertEquals(r, t);
            // -(-r) = r
            t = zl64.neg(zl64.neg(r));
            Assert.assertEquals(r, t);
            // r + s - s = r
            t = zl64.sub(zl64.add(r, s), s);
            Assert.assertEquals(r, t);
            // r - s + s = r
            t = zl64.add(zl64.sub(r, s), s);
            Assert.assertEquals(r, t);
            // (-r) + r = 0
            t = zl64.add(r, zl64.neg(r));
            Assert.assertEquals(zero, t);
            // r - r = 0
            t = zl64.sub(r, r);
            Assert.assertEquals(zero, t);
        }
    }

    @Test
    public void testConstantMul() {
        long zero = zl64.createZero();
        long one = zl64.createOne();
        long p;
        long t;
        // 0 * 0 = 0
        p = zl64.createZero();
        t = zl64.mul(p, zero);
        Assert.assertEquals(zero, t);
        // 0 * 1 = 0
        p = zl64.createZero();
        t = zl64.mul(p, one);
        Assert.assertEquals(zero, t);
        // 1 * 0 = 0
        p = zl64.createOne();
        t = zl64.mul(p, zero);
        Assert.assertEquals(zero, t);
        // 1 * 1 = 1
        p = zl64.createOne();
        t = zl64.mul(p, one);
        Assert.assertEquals(one, t);
    }

    @Test
    public void testRandomMul() {
        long zero = zl64.createZero();
        long one = zl64.createOne();
        long r;
        long t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r * 0 = 0
            r = zl64.createRandom(secureRandom);
            t = zl64.mul(r, zero);
            Assert.assertEquals(zero, t);
            // r * 1 = r
            r = zl64.createNonZeroRandom(secureRandom);
            t = zl64.mul(r, one);
            Assert.assertEquals(r, t);
        }
    }

    @Test
    public void testConstantModPow() {
        long zero = zl64.createZero();
        long one = zl64.createOne();
        // 0^0 = 1
        Assert.assertEquals(one, zl64.pow(zero, zero));
        // 0^1 = 0
        Assert.assertEquals(zero, zl64.pow(zero, one));
        // 1^0 = 1
        Assert.assertEquals(one, zl64.pow(one, zero));
        // 1^1 = 1
        Assert.assertEquals(one, zl64.pow(one, one));
    }

    @Test
    public void testRandomModPow() {
        long zero = zl64.createZero();
        long one = zl64.createOne();
        for (int round = 0; round < MAX_RANDOM; round++) {
            long e = Integer.toUnsignedLong(secureRandom.nextInt());
            // 0^e = 0
            Assert.assertEquals(zero, zl64.pow(zero, e));
            // 1^e = 1
            Assert.assertEquals(one, zl64.pow(one, e));

            long a = zl64.createNonZeroRandom(secureRandom);
            // a^0 = 1
            Assert.assertEquals(one, zl64.pow(a, zero));
            // a^1 = a
            Assert.assertEquals(a, zl64.pow(a, one));
        }
    }

    @Test
    public void testAddParallel() {
        long p = zl64.createNonZeroRandom(secureRandom);
        long q = zl64.createNonZeroRandom(secureRandom);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zl64.add(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);
    }

    @Test
    public void testNegParallel() {
        long p = zl64.createNonZeroRandom(secureRandom);
        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zl64.neg(p))
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);
    }

    @Test
    public void testSubParallel() {
        long p = zl64.createNonZeroRandom(secureRandom);
        long q = zl64.createNonZeroRandom(secureRandom);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zl64.sub(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);
    }

    @Test
    public void testMulParallel() {
        long p = zl64.createNonZeroRandom(secureRandom);
        long q = zl64.createNonZeroRandom(secureRandom);
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .mapToLong(index -> zl64.mul(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1, mulCount);
    }


    @Test
    public void testModPowParallel() {
        long p = zl64.createNonZeroRandom(secureRandom);
        long e = Integer.toUnsignedLong(secureRandom.nextInt());
        long mulPowCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zl64.pow(p, e))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulPowCount);
    }
}
