package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
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
 * LongField tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class LongFieldTest {
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

        // Zp64
        Zp64Type[] zp64Types = new Zp64Type[]{Zp64Type.RINGS};
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62};
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
     * the LongField instance
     */
    private final LongField longField;

    public LongFieldTest(String name, LongField longField) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.longField = longField;
    }

    @Test
    public void testIllegalInputs() {
        int l = longField.getL();
        // try operating p and q when p is invalid
        final long largeP = 1L << (l + 1);
        final long negativeP = -1L;
        final long q = longField.createNonZeroRandom(SECURE_RANDOM);
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> longField.div(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> longField.div(negativeP, q));

        // try operating p and q when q is invalid
        final long p = longField.createNonZeroRandom(SECURE_RANDOM);
        final long largeQ = 1L << (l + 1);
        final long negativeQ = -1L;
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> longField.div(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> longField.div(p, negativeQ));

        // try operating p when p is invalid
        // try inverting p
        Assert.assertThrows(AssertionError.class, () -> longField.inv(largeP));
        Assert.assertThrows(AssertionError.class, () -> longField.inv(negativeP));
    }

    @Test
    public void testConstantMulInvDiv() {
        long zero = longField.createZero();
        long one = longField.createOne();
        long p;
        long t;
        // 0 / 1 = 0
        p = longField.createZero();
        t = longField.div(p, one);
        Assert.assertEquals(zero, t);
        // 1^{-1} = 1
        p = longField.createOne();
        t = longField.inv(p);
        Assert.assertEquals(one, t);
        // 1 / 1 = 1
        p = longField.createOne();
        t = longField.div(p, one);
        Assert.assertEquals(one, t);
    }

    @Test
    public void testRandomMulInvDiv() {
        long one = longField.createOne();
        long r;
        long t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r / 1 = r
            r = longField.createNonZeroRandom(SECURE_RANDOM);
            t = longField.div(r, one);
            Assert.assertEquals(r, t);
            // r / r = 1
            r = longField.createNonZeroRandom(SECURE_RANDOM);
            t = longField.div(r, r);
            Assert.assertEquals(one, t);
            // r * (r^{-1}) = 1
            r = longField.createNonZeroRandom(SECURE_RANDOM);
            t = longField.mul(r, longField.inv(r));
            Assert.assertEquals(one, t);
            // 1 / a = a^{-1}
            Assert.assertEquals(longField.div(longField.createOne(), r), longField.inv(r));
        }
    }

    @Test
    public void testRandomPowMulInv() {
        long r;
        long s;
        long t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            r = longField.createNonZeroRandom(SECURE_RANDOM);
            s = longField.createNonZeroRandom(SECURE_RANDOM);
            t = longField.createNonZeroRandom(SECURE_RANDOM);
            // (r^s)^(-1) = (r^(-1))^s
            Assert.assertEquals(
                longField.inv(longField.pow(r, s)),
                longField.pow(longField.inv(r), s)
            );
            // (r^s)^t = (r^t)^s
            Assert.assertEquals(
                longField.pow(longField.pow(r, s), t),
                longField.pow(longField.pow(r, t), s)
            );
        }

    }

    @Test
    public void testInvParallel() {
        long p = longField.createNonZeroRandom(SECURE_RANDOM);
        long invCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> longField.inv(p))
            .distinct()
            .count();
        Assert.assertEquals(1L, invCount);
    }

    @Test
    public void testDivParallel() {
        long p = longField.createNonZeroRandom(SECURE_RANDOM);
        long q = longField.createNonZeroRandom(SECURE_RANDOM);
        long divCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> longField.div(p, q))
            .distinct()
            .count();
        Assert.assertEquals(1L, divCount);
    }
}
