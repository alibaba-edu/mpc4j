package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory.ZlType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zn.ZnFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zn.ZnFactory.ZnType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory.ZpType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * BigIntegerRing tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class BigIntegerRingTest {
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

        // Zn
        ZnType[] types = new ZnType[]{ZnFactory.ZnType.JDK};
        long[] ns = new long[]{2, 3, 4, 7, 8, 247, 350, 511, 512, 513, 701, 833, 991, 1023, 1024, 1025};
        for (ZnType type : types) {
            // add each n
            for (long n : ns) {
                configurations.add(new Object[]{
                    ZnType.class.getSimpleName() + " (" + type.name() + " ,n = " + n,
                    ZnFactory.createInstance(EnvType.STANDARD, type, BigInteger.valueOf(n)),
                });
            }
        }

        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62, 63, 64, 65, 127, 128, 129};
        // Zl
        ZlType[] zlTypes = new ZlType[]{ZlType.JDK};
        for (ZlType type : zlTypes) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{
                    ZlType.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")",
                    ZlFactory.createInstance(EnvType.STANDARD, type, l),
                });
            }
        }
        // Zp
        ZpType[] zpTypes = new ZpType[]{ZpType.JDK};
        for (ZpType type : zpTypes) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{
                    ZpType.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")",
                    ZpFactory.createInstance(EnvType.STANDARD, type, l),
                });
            }
        }

        return configurations;
    }

    /**
     * the BigIntegerRing instance
     */
    private final BigIntegerRing bigIntegerRing;

    public BigIntegerRingTest(String name, BigIntegerRing bigIntegerRing) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.bigIntegerRing = bigIntegerRing;
    }

    @Test
    public void testIllegalInputs() {
        int l = bigIntegerRing.getL();
        // try operating p and q when p is invalid
        final BigInteger largeP = BigInteger.ONE.shiftLeft(l + 1);
        final BigInteger negativeP = BigInteger.ONE.negate();
        final BigInteger q = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        // try adding
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.add(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.add(negativeP, q));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.sub(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.sub(negativeP, q));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.mul(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.mul(negativeP, q));

        // try operating p and q when q is invalid
        final BigInteger p = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        final BigInteger largeQ = BigInteger.ONE.shiftLeft(l + 1);
        final BigInteger negativeQ = BigInteger.ONE.negate();
        // try adding
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.add(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.add(p, negativeQ));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.sub(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.sub(p, negativeQ));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.mul(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.sub(p, negativeQ));

        // try operating p when p is invalid
        // try negating p
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.neg(largeP));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerRing.neg(negativeP));
    }

    @Test
    public void testCreateZero() {
        BigInteger zero = bigIntegerRing.createZero();
        Assert.assertTrue(bigIntegerRing.isZero(zero));
        Assert.assertFalse(bigIntegerRing.isOne(zero));
    }

    @Test
    public void testCreateOne() {
        BigInteger one = bigIntegerRing.createOne();
        Assert.assertTrue(bigIntegerRing.isOne(one));
        Assert.assertFalse(bigIntegerRing.isZero(one));
    }

    @Test
    public void testCreateRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        // create random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            BigInteger randomElement = bigIntegerRing.createRandom(SECURE_RANDOM);
            Assert.assertTrue(bigIntegerRing.validateElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                BigInteger randomElement = bigIntegerRing.createRandom(seed);
                Assert.assertTrue(bigIntegerRing.validateElement(randomElement));
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
            BigInteger randomNonZeroElement = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertTrue(bigIntegerRing.validateElement(randomNonZeroElement));
            Assert.assertTrue(bigIntegerRing.validateNonZeroElement(randomNonZeroElement));
            Assert.assertFalse(bigIntegerRing.isZero(randomNonZeroElement));
        });
        // create non-zero random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                BigInteger randomNonZeroElement = bigIntegerRing.createNonZeroRandom(seed);
                Assert.assertTrue(bigIntegerRing.validateElement(randomNonZeroElement));
                Assert.assertTrue(bigIntegerRing.validateNonZeroElement(randomNonZeroElement));
                Assert.assertFalse(bigIntegerRing.isZero(randomNonZeroElement));
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
            BigInteger randomElement = bigIntegerRing.createRangeRandom(SECURE_RANDOM);
            Assert.assertTrue(bigIntegerRing.validateElement(randomElement));
            Assert.assertTrue(bigIntegerRing.validateRangeElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                BigInteger randomRangeElement = bigIntegerRing.createRangeRandom(seed);
                Assert.assertTrue(bigIntegerRing.validateElement(randomRangeElement));
                Assert.assertTrue(bigIntegerRing.validateRangeElement(randomRangeElement));
                return randomRangeElement;
            })
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testConstantAddNegSub() {
        BigInteger zero = bigIntegerRing.createZero();
        BigInteger p;
        BigInteger t;
        // 0 + 0 = 0
        p = bigIntegerRing.createZero();
        t = bigIntegerRing.add(p, zero);
        Assert.assertEquals(zero, t);
        // -0 = 0
        p = bigIntegerRing.createZero();
        t = bigIntegerRing.neg(p);
        Assert.assertEquals(zero, t);
        // 0 - 0 = 0
        p = bigIntegerRing.createZero();
        t = bigIntegerRing.sub(p, zero);
        Assert.assertEquals(zero, t);
    }

    @Test
    public void testRandomAddNegSub() {
        BigInteger zero = bigIntegerRing.createZero();
        BigInteger r;
        BigInteger s;
        BigInteger t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            r = bigIntegerRing.createRandom(SECURE_RANDOM);
            s = bigIntegerRing.createRandom(SECURE_RANDOM);
            // r + 0 = r
            t = bigIntegerRing.add(r, zero);
            Assert.assertEquals(r, t);
            // r - 0 = r
            t = bigIntegerRing.sub(r, zero);
            Assert.assertEquals(r, t);
            // -(-r) = r
            t = bigIntegerRing.neg(bigIntegerRing.neg(r));
            Assert.assertEquals(r, t);
            // r + s - s = r
            t = bigIntegerRing.sub(bigIntegerRing.add(r, s), s);
            Assert.assertEquals(r, t);
            // r - s + s = r
            t = bigIntegerRing.add(bigIntegerRing.sub(r, s), s);
            Assert.assertEquals(r, t);
            // (-r) + r = 0
            t = bigIntegerRing.add(r, bigIntegerRing.neg(r));
            Assert.assertEquals(zero, t);
            // r - r = 0
            t = bigIntegerRing.sub(r, r);
            Assert.assertEquals(zero, t);
        }
    }

    @Test
    public void testConstantMul() {
        BigInteger zero = bigIntegerRing.createZero();
        BigInteger one = bigIntegerRing.createOne();
        BigInteger p;
        BigInteger t;
        // 0 * 0 = 0
        p = bigIntegerRing.createZero();
        t = bigIntegerRing.mul(p, zero);
        Assert.assertEquals(zero, t);
        // 0 * 1 = 0
        p = bigIntegerRing.createZero();
        t = bigIntegerRing.mul(p, one);
        Assert.assertEquals(zero, t);
        // 1 * 0 = 0
        p = bigIntegerRing.createOne();
        t = bigIntegerRing.mul(p, zero);
        Assert.assertEquals(zero, t);
        // 1 * 1 = 1
        p = bigIntegerRing.createOne();
        t = bigIntegerRing.mul(p, one);
        Assert.assertEquals(one, t);
    }

    @Test
    public void testRandomMul() {
        BigInteger zero = bigIntegerRing.createZero();
        BigInteger one = bigIntegerRing.createOne();
        BigInteger r;
        BigInteger t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r * 0 = 0
            r = bigIntegerRing.createRandom(SECURE_RANDOM);
            t = bigIntegerRing.mul(r, zero);
            Assert.assertEquals(zero, t);
            // r * 1 = r
            r = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
            t = bigIntegerRing.mul(r, one);
            Assert.assertEquals(r, t);
        }
    }

    @Test
    public void testConstantModPow() {
        BigInteger zero = bigIntegerRing.createZero();
        BigInteger one = bigIntegerRing.createOne();
        // 0^0 = 1
        Assert.assertEquals(one, bigIntegerRing.pow(zero, zero));
        // 0^1 = 0
        Assert.assertEquals(zero, bigIntegerRing.pow(zero, one));
        // 1^0 = 1
        Assert.assertEquals(one, bigIntegerRing.pow(one, zero));
        // 1^1 = 1
        Assert.assertEquals(one, bigIntegerRing.pow(one, one));
    }

    @Test
    public void testRandomModPow() {
        BigInteger zero = bigIntegerRing.createZero();
        BigInteger one = bigIntegerRing.createOne();
        for (int round = 0; round < MAX_RANDOM; round++) {
            // 0^a = 0
            BigInteger a = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertEquals(zero, bigIntegerRing.pow(zero, a));
            // a^0 = 1
            Assert.assertEquals(one, bigIntegerRing.pow(a, zero));
            // a^1 = a
            Assert.assertEquals(a, bigIntegerRing.pow(a, one));
            // 1^a = 1
            Assert.assertEquals(one, bigIntegerRing.pow(one, a));
        }
    }

    @Test
    public void testAddParallel() {
        BigInteger p = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        BigInteger q = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> bigIntegerRing.add(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);
    }

    @Test
    public void testNegParallel() {
        BigInteger p = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> bigIntegerRing.neg(p))
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);
    }

    @Test
    public void testSubParallel() {
        BigInteger p = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        BigInteger q = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> bigIntegerRing.sub(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);
    }

    @Test
    public void testMulParallel() {
        BigInteger p = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        BigInteger q = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> bigIntegerRing.mul(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1, mulCount);
    }


    @Test
    public void testModPowParallel() {
        BigInteger p = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        BigInteger q = bigIntegerRing.createNonZeroRandom(SECURE_RANDOM);
        long mulPowCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> bigIntegerRing.pow(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulPowCount);
    }
}
