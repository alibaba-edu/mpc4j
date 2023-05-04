package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory.Zl64Type;
import edu.alibaba.mpc4j.common.tool.galoisfield.zn64.Zn64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zn64.Zn64Factory.Zn64Type;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory.Zp64Type;
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
 * LongRing tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class LongRingTest {
    /**
     * parallel num
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * random test num
     */
    private static final int MAX_RANDOM = 400;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Zn64
        long[] ns = new long[]{2, 3, 4, 7, 8, 247, 350, 511, 512, 513, 701, 833, 991, 1023, 1024, 1025};
        Zn64Type[] zn64Types = new Zn64Type[]{Zn64Type.RINGS};
        for (Zn64Type type : zn64Types) {
            // add each l
            for (long n : ns) {
                configurations.add(new Object[]{
                    Zn64Type.class.getSimpleName() + " (" + type.name() + ", n = " + n + ")",
                    Zn64Factory.createInstance(EnvType.STANDARD, type, n),
                });
            }
        }
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62};
        // Zl64
        Zl64Type[] zl64Types = new Zl64Type[]{Zl64Type.JDK, Zl64Type.RINGS};
        for (Zl64Type type : zl64Types) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{
                    Zl64Type.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")",
                    Zl64Factory.createInstance(EnvType.STANDARD, type, l),
                });
            }
        }
        // Zp64
        Zp64Type[] zp64Types = new Zp64Type[]{Zp64Type.RINGS};
        for (Zp64Type type : zp64Types) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{
                    Zp64Type.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")",
                    Zp64Factory.createInstance(EnvType.STANDARD, type, l),
                });
            }
        }

        return configurations;
    }

    /**
     * the LongRing instance
     */
    private final LongRing longRing;

    public LongRingTest(String name, LongRing longRing) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.longRing = longRing;
    }

    @Test
    public void testIllegalInputs() {
        int l = longRing.getL();
        // try operating p and q when p is invalid
        final long largeP = (1L << (l + 1));
        final long negativeP = -1L;
        final long q = longRing.createNonZeroRandom(SECURE_RANDOM);
        // try adding
        Assert.assertThrows(AssertionError.class, () -> longRing.add(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> longRing.add(negativeP, q));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> longRing.sub(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> longRing.sub(negativeP, q));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> longRing.mul(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> longRing.mul(negativeP, q));

        // try operating p and q when q is invalid
        final long p = longRing.createNonZeroRandom(SECURE_RANDOM);
        final long largeQ = (1L << (l + 1));
        final long negativeQ = -1L;
        // try adding
        Assert.assertThrows(AssertionError.class, () -> longRing.add(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> longRing.add(p, negativeQ));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> longRing.sub(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> longRing.sub(p, negativeQ));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> longRing.mul(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> longRing.sub(p, negativeQ));

        // try operating p when p is invalid
        // try negating p
        Assert.assertThrows(AssertionError.class, () -> longRing.neg(largeP));
        Assert.assertThrows(AssertionError.class, () -> longRing.neg(negativeP));
    }

    @Test
    public void testCreateZero() {
        long zero = longRing.createZero();
        Assert.assertTrue(longRing.isZero(zero));
        Assert.assertFalse(longRing.isOne(zero));
    }

    @Test
    public void testCreateOne() {
        long one = longRing.createOne();
        Assert.assertTrue(longRing.isOne(one));
        Assert.assertFalse(longRing.isZero(one));
    }

    @Test
    public void testCreateRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        // create random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            long randomElement = longRing.createRandom(SECURE_RANDOM);
            Assert.assertTrue(longRing.validateElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                long randomElement = longRing.createRandom(seed);
                Assert.assertTrue(longRing.validateElement(randomElement));
                return randomElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testCreateNonZeroRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        // create non-zero random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            long randomNonZeroElement = longRing.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertTrue(longRing.validateElement(randomNonZeroElement));
            Assert.assertTrue(longRing.validateNonZeroElement(randomNonZeroElement));
            Assert.assertFalse(longRing.isZero(randomNonZeroElement));
        });
        // create non-zero random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                long randomNonZeroElement = longRing.createNonZeroRandom(seed);
                Assert.assertTrue(longRing.validateElement(randomNonZeroElement));
                Assert.assertTrue(longRing.validateNonZeroElement(randomNonZeroElement));
                Assert.assertFalse(longRing.isZero(randomNonZeroElement));
                return randomNonZeroElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testCreateRangeRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        // create range random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            long randomElement = longRing.createRangeRandom(SECURE_RANDOM);
            Assert.assertTrue(longRing.validateElement(randomElement));
            Assert.assertTrue(longRing.validateRangeElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                long randomRangeElement = longRing.createRangeRandom(seed);
                Assert.assertTrue(longRing.validateElement(randomRangeElement));
                Assert.assertTrue(longRing.validateRangeElement(randomRangeElement));
                return randomRangeElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testConstantAddNegSub() {
        long zero = longRing.createZero();
        long p;
        long t;
        // 0 + 0 = 0
        p = longRing.createZero();
        t = longRing.add(p, zero);
        Assert.assertEquals(zero, t);
        // -0 = 0
        p = longRing.createZero();
        t = longRing.neg(p);
        Assert.assertEquals(zero, t);
        // 0 - 0 = 0
        p = longRing.createZero();
        t = longRing.sub(p, zero);
        Assert.assertEquals(zero, t);
    }

    @Test
    public void testRandomAddNegSub() {
        long zero = longRing.createZero();
        long r;
        long s;
        long t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            r = longRing.createRandom(SECURE_RANDOM);
            s = longRing.createRandom(SECURE_RANDOM);
            // r + 0 = r
            t = longRing.add(r, zero);
            Assert.assertEquals(r, t);
            // r - 0 = r
            t = longRing.sub(r, zero);
            Assert.assertEquals(r, t);
            // -(-r) = r
            t = longRing.neg(longRing.neg(r));
            Assert.assertEquals(r, t);
            // r + s - s = r
            t = longRing.sub(longRing.add(r, s), s);
            Assert.assertEquals(r, t);
            // r - s + s = r
            t = longRing.add(longRing.sub(r, s), s);
            Assert.assertEquals(r, t);
            // (-r) + r = 0
            t = longRing.add(r, longRing.neg(r));
            Assert.assertEquals(zero, t);
            // r - r = 0
            t = longRing.sub(r, r);
            Assert.assertEquals(zero, t);
        }
    }

    @Test
    public void testConstantMul() {
        long zero = longRing.createZero();
        long one = longRing.createOne();
        long p;
        long t;
        // 0 * 0 = 0
        p = longRing.createZero();
        t = longRing.mul(p, zero);
        Assert.assertEquals(zero, t);
        // 0 * 1 = 0
        p = longRing.createZero();
        t = longRing.mul(p, one);
        Assert.assertEquals(zero, t);
        // 1 * 0 = 0
        p = longRing.createOne();
        t = longRing.mul(p, zero);
        Assert.assertEquals(zero, t);
        // 1 * 1 = 1
        p = longRing.createOne();
        t = longRing.mul(p, one);
        Assert.assertEquals(one, t);
    }

    @Test
    public void testRandomMul() {
        long zero = longRing.createZero();
        long one = longRing.createOne();
        long r;
        long t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r * 0 = 0
            r = longRing.createRandom(SECURE_RANDOM);
            t = longRing.mul(r, zero);
            Assert.assertEquals(zero, t);
            // r * 1 = r
            r = longRing.createNonZeroRandom(SECURE_RANDOM);
            t = longRing.mul(r, one);
            Assert.assertEquals(r, t);
        }
    }

    @Test
    public void testConstantModPow() {
        long zero = longRing.createZero();
        long one = longRing.createOne();
        // 0^0 = 1
        Assert.assertEquals(one, longRing.pow(zero, zero));
        // 0^1 = 0
        Assert.assertEquals(zero, longRing.pow(zero, one));
        // 1^0 = 1
        Assert.assertEquals(one, longRing.pow(one, zero));
        // 1^1 = 1
        Assert.assertEquals(one, longRing.pow(one, one));
    }

    @Test
    public void testRandomModPow() {
        long zero = longRing.createZero();
        long one = longRing.createOne();
        for (int round = 0; round < MAX_RANDOM; round++) {
            // 0^a = 0
            long a = longRing.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertEquals(zero, longRing.pow(zero, a));
            // a^0 = 1
            Assert.assertEquals(one, longRing.pow(a, zero));
            // a^1 = a
            Assert.assertEquals(a, longRing.pow(a, one));
            // 1^a = 1
            Assert.assertEquals(one, longRing.pow(one, a));
        }
    }

    @Test
    public void testAddParallel() {
        long p = longRing.createNonZeroRandom(SECURE_RANDOM);
        long q = longRing.createNonZeroRandom(SECURE_RANDOM);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> longRing.add(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);
    }

    @Test
    public void testNegParallel() {
        long p = longRing.createNonZeroRandom(SECURE_RANDOM);
        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> longRing.neg(p))
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);
    }

    @Test
    public void testSubParallel() {
        long p = longRing.createNonZeroRandom(SECURE_RANDOM);
        long q = longRing.createNonZeroRandom(SECURE_RANDOM);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> longRing.sub(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);
    }

    @Test
    public void testMulParallel() {
        long p = longRing.createNonZeroRandom(SECURE_RANDOM);
        long q = longRing.createNonZeroRandom(SECURE_RANDOM);
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .mapToLong(index -> longRing.mul(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1, mulCount);
    }


    @Test
    public void testModPowParallel() {
        long p = longRing.createNonZeroRandom(SECURE_RANDOM);
        long q = longRing.createNonZeroRandom(SECURE_RANDOM);
        long mulPowCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> longRing.pow(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulPowCount);
    }
}
