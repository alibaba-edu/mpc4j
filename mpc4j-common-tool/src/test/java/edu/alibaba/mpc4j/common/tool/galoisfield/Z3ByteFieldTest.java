package edu.alibaba.mpc4j.common.tool.galoisfield;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Z3 byte field test.
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public class Z3ByteFieldTest {
    /**
     * random test num
     */
    private static final int MAX_RANDOM = 1000;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;

    public Z3ByteFieldTest() {
        z3Field = new Z3ByteField();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // try operating p and q when p is invalid
        final byte largeP = 3;
        final byte negativeP = -1;
        final byte zeroP = 0;
        final byte q = 1;
        // try adding
        Assert.assertThrows(AssertionError.class, () -> z3Field.add(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> z3Field.add(negativeP, q));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> z3Field.sub(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> z3Field.sub(negativeP, q));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> z3Field.mul(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> z3Field.mul(negativeP, q));
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> z3Field.div(largeP, q));
        Assert.assertThrows(AssertionError.class, () -> z3Field.div(negativeP, q));

        // try operating p and q when q is invalid
        final byte p = 1;
        final byte largeQ = 3;
        final byte negativeQ = -1;
        final byte zeroQ = 0;
        // try adding
        Assert.assertThrows(AssertionError.class, () -> z3Field.add(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> z3Field.add(p, negativeQ));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> z3Field.sub(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> z3Field.sub(p, negativeQ));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> z3Field.mul(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> z3Field.mul(p, negativeQ));
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> z3Field.div(p, largeQ));
        Assert.assertThrows(AssertionError.class, () -> z3Field.div(p, negativeQ));
        Assert.assertThrows(AssertionError.class, () -> z3Field.div(p, zeroQ));

        // try operating p when p is invalid
        // try negating p
        Assert.assertThrows(AssertionError.class, () -> z3Field.neg(largeP));
        Assert.assertThrows(AssertionError.class, () -> z3Field.neg(negativeP));
        // try inverting p
        Assert.assertThrows(AssertionError.class, () -> z3Field.inv(largeP));
        Assert.assertThrows(AssertionError.class, () -> z3Field.inv(negativeP));
        Assert.assertThrows(AssertionError.class, () -> z3Field.inv(zeroP));
    }

    @Test
    public void testCreateZero() {
        byte zero = z3Field.createZero();
        Assert.assertTrue(z3Field.isZero(zero));
        Assert.assertFalse(z3Field.isOne(zero));
    }

    @Test
    public void testCreateOne() {
        byte one = z3Field.createOne();
        Assert.assertTrue(z3Field.isOne(one));
        Assert.assertFalse(z3Field.isZero(one));
    }

    @Test
    public void testCreateRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        // create random
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToLong(index -> {
                byte randomElement = z3Field.createRandom(secureRandom);
                Assert.assertTrue(z3Field.validateElement(randomElement));
                return randomElement;
            })
            .distinct()
            .count();
        // very high probability that we have 3 distinct results
        Assert.assertEquals(z3Field.getPrime(), randomNum);
    }

    @Test
    public void testCreateNonZeroRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        // create non-zero random
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToLong(index -> {
                byte randomNonZeroElement = z3Field.createNonZeroRandom(secureRandom);
                Assert.assertTrue(z3Field.validateElement(randomNonZeroElement));
                Assert.assertTrue(z3Field.validateNonZeroElement(randomNonZeroElement));
                Assert.assertFalse(z3Field.isZero(randomNonZeroElement));
                return randomNonZeroElement;
            })
            .distinct()
            .count();
        // very high probability that we have 2 distinct results
        Assert.assertEquals(z3Field.getPrime() - 1, randomNum);
    }

    @Test
    public void testAdd() {
        Assert.assertEquals(0, z3Field.add((byte) 0, (byte) 0));
        Assert.assertEquals(1, z3Field.add((byte) 0, (byte) 1));
        Assert.assertEquals(2, z3Field.add((byte) 0, (byte) 2));
        Assert.assertEquals(1, z3Field.add((byte) 1, (byte) 0));
        Assert.assertEquals(2, z3Field.add((byte) 1, (byte) 1));
        Assert.assertEquals(0, z3Field.add((byte) 1, (byte) 2));
        Assert.assertEquals(2, z3Field.add((byte) 2, (byte) 0));
        Assert.assertEquals(0, z3Field.add((byte) 2, (byte) 1));
        Assert.assertEquals(1, z3Field.add((byte) 2, (byte) 2));
    }

    @Test
    public void testNeg() {
        Assert.assertEquals(0, z3Field.neg((byte) 0));
        Assert.assertEquals(2, z3Field.neg((byte) 1));
        Assert.assertEquals(1, z3Field.neg((byte) 2));
    }

    @Test
    public void testSub() {
        Assert.assertEquals(0, z3Field.sub((byte) 0, (byte) 0));
        Assert.assertEquals(2, z3Field.sub((byte) 0, (byte) 1));
        Assert.assertEquals(1, z3Field.sub((byte) 0, (byte) 2));
        Assert.assertEquals(1, z3Field.sub((byte) 1, (byte) 0));
        Assert.assertEquals(0, z3Field.sub((byte) 1, (byte) 1));
        Assert.assertEquals(2, z3Field.sub((byte) 1, (byte) 2));
        Assert.assertEquals(2, z3Field.sub((byte) 2, (byte) 0));
        Assert.assertEquals(1, z3Field.sub((byte) 2, (byte) 1));
        Assert.assertEquals(0, z3Field.sub((byte) 2, (byte) 2));
    }

    @Test
    public void testMul() {
        Assert.assertEquals(0, z3Field.mul((byte) 0, (byte) 0));
        Assert.assertEquals(0, z3Field.mul((byte) 0, (byte) 1));
        Assert.assertEquals(0, z3Field.mul((byte) 0, (byte) 2));
        Assert.assertEquals(0, z3Field.mul((byte) 1, (byte) 0));
        Assert.assertEquals(1, z3Field.mul((byte) 1, (byte) 1));
        Assert.assertEquals(2, z3Field.mul((byte) 1, (byte) 2));
        Assert.assertEquals(0, z3Field.mul((byte) 2, (byte) 0));
        Assert.assertEquals(2, z3Field.mul((byte) 2, (byte) 1));
        Assert.assertEquals(1, z3Field.mul((byte) 2, (byte) 2));
    }

    @Test
    public void testInv() {
        Assert.assertEquals(1, z3Field.inv((byte) 1));
        Assert.assertEquals(2, z3Field.inv((byte) 2));
    }

    @Test
    public void testDiv() {
        Assert.assertEquals(0, z3Field.div((byte) 0, (byte) 1));
        Assert.assertEquals(0, z3Field.div((byte) 0, (byte) 2));
        Assert.assertEquals(1, z3Field.div((byte) 1, (byte) 1));
        Assert.assertEquals(2, z3Field.div((byte) 1, (byte) 2));
        Assert.assertEquals(2, z3Field.div((byte) 2, (byte) 1));
        Assert.assertEquals(1, z3Field.div((byte) 2, (byte) 2));
    }
}
