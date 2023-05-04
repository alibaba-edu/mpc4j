package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
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
 * BigIntegerField tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class BigIntegerFieldTest {
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

        // Zp
        ZpType[] zpTypes = new ZpType[]{ZpType.JDK};
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62, 63, 64, 65, 127, 128, 129};
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
     * the BigIntegerField instance
     */
    private final BigIntegerField bigIntegerField;

    public BigIntegerFieldTest(String name, BigIntegerField bigIntegerField) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.bigIntegerField = bigIntegerField;
    }

    @Test
    public void testIllegalInputs() {
        int l = bigIntegerField.getL();
        // try operating p and q when p is invalid
        final BigInteger largeP = BigInteger.ONE.shiftLeft(l + 1);
        final BigInteger negativeP = BigInteger.ONE.negate();
        final BigInteger q = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> bigIntegerField.div(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerField.div(negativeP, q));

        // try operating p and q when q is invalid
        final BigInteger p = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
        final BigInteger largeQ = BigInteger.ONE.shiftLeft(l + 1);
        final BigInteger negativeQ = BigInteger.ONE.negate();
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> bigIntegerField.div(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerField.div(p, negativeQ));

        // try operating p when p is invalid
        // try inverting p
        Assert.assertThrows(AssertionError.class, () -> bigIntegerField.inv(largeP));
        Assert.assertThrows(AssertionError.class, () -> bigIntegerField.inv(negativeP));
    }

    @Test
    public void testConstantMulInvDiv() {
        BigInteger zero = bigIntegerField.createZero();
        BigInteger one = bigIntegerField.createOne();
        BigInteger p;
        BigInteger t;
        // 0 / 1 = 0
        p = bigIntegerField.createZero();
        t = bigIntegerField.div(p, one);
        Assert.assertEquals(zero, t);
        // 1^{-1} = 1
        p = bigIntegerField.createOne();
        t = bigIntegerField.inv(p);
        Assert.assertEquals(one, t);
        // 1 / 1 = 1
        p = bigIntegerField.createOne();
        t = bigIntegerField.div(p, one);
        Assert.assertEquals(one, t);
    }

    @Test
    public void testRandomMulInvDiv() {
        BigInteger one = bigIntegerField.createOne();
        BigInteger r;
        BigInteger t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r / 1 = r
            r = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
            t = bigIntegerField.div(r, one);
            Assert.assertEquals(r, t);
            // r / r = 1
            r = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
            t = bigIntegerField.div(r, r);
            Assert.assertEquals(one, t);
            // r * (r^{-1}) = 1
            r = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
            t = bigIntegerField.mul(r, bigIntegerField.inv(r));
            Assert.assertEquals(one, t);
            // 1 / a = a^{-1}
            Assert.assertEquals(bigIntegerField.div(bigIntegerField.createOne(), r), bigIntegerField.inv(r));
        }
    }

    @Test
    public void testRandomPowMulInv() {
        BigInteger r;
        BigInteger s;
        BigInteger t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            r = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
            s = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
            t = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
            // (r^s)^(-1) = (r^(-1))^s
            Assert.assertEquals(
                bigIntegerField.inv(bigIntegerField.pow(r, s)),
                bigIntegerField.pow(bigIntegerField.inv(r), s)
            );
            // (r^s)^t = (r^t)^s
            Assert.assertEquals(
                bigIntegerField.pow(bigIntegerField.pow(r, s), t),
                bigIntegerField.pow(bigIntegerField.pow(r, t), s)
            );
        }

    }

    @Test
    public void testInvParallel() {
        BigInteger p = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
        long invCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> bigIntegerField.inv(p))
            .distinct()
            .count();
        Assert.assertEquals(1L, invCount);
    }

    @Test
    public void testDivParallel() {
        BigInteger p = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
        BigInteger q = bigIntegerField.createNonZeroRandom(SECURE_RANDOM);
        long divCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> bigIntegerField.div(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, divCount);
    }
}
