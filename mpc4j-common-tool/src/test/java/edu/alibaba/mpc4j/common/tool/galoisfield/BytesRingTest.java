package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * BytesRing tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class BytesRingTest {
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

        // GF64
        for (Gf64Type type : Gf64Type.values()) {
            configurations.add(new Object[]{
                Gf64Type.class.getSimpleName() + " (" + type.name() + ")",
                Gf64Factory.createInstance(EnvType.STANDARD, type),
            });
        }
        // GF2K
        for (Gf2kType type : Gf2kType.values()) {
            configurations.add(new Object[]{
                Gf2kType.class.getSimpleName() + " (" + type.name() + ")",
                Gf2kFactory.createInstance(EnvType.STANDARD, type),
            });
        }
        // GF2E
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 128, 256};
        for (Gf2eType type : Gf2eType.values()) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{
                    Gf2eType.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")",
                    Gf2eFactory.createInstance(EnvType.STANDARD, type, l),
                });
            }
        }

        return configurations;
    }

    /**
     * the BytesRing instance
     */
    private final BytesRing bytesRing;
    /**
     * the element byte length
     */
    private final int elementByteLength;

    public BytesRingTest(String name, BytesRing bytesRing) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.bytesRing = bytesRing;
        elementByteLength = bytesRing.getElementByteLength();
    }

    @Test
    public void testIllegalInputs() {
        // try operating p and q when p is invalid
        final byte[] invalidP = new byte[elementByteLength - 1];
        SECURE_RANDOM.nextBytes(invalidP);
        final byte[] q = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        // try adding
        Assert.assertThrows(AssertionError.class, () -> bytesRing.add(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.addi(invalidP, q));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> bytesRing.sub(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.subi(invalidP, q));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> bytesRing.mul(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.muli(invalidP, q));

        // try operating p and q when q is invalid
        final byte[] p = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        final byte[] invalidQ = new byte[elementByteLength - 1];
        SECURE_RANDOM.nextBytes(invalidQ);
        // try adding
        Assert.assertThrows(AssertionError.class, () -> bytesRing.add(p, invalidQ));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.addi(p, invalidQ));
        // try subtracting
        Assert.assertThrows(AssertionError.class, () -> bytesRing.sub(p, invalidQ));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.subi(p, invalidQ));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> bytesRing.mul(p, invalidQ));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.muli(p, invalidQ));

        // try operating p when p is invalid
        // try negating p
        Assert.assertThrows(AssertionError.class, () -> bytesRing.neg(invalidP));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.negi(invalidP));
    }

    @Test
    public void testCreateZero() {
        byte[] zero = bytesRing.createZero();
        Assert.assertTrue(bytesRing.isZero(zero));
        Assert.assertFalse(bytesRing.isOne(zero));
    }

    @Test
    public void testCreateOne() {
        byte[] one = bytesRing.createOne();
        Assert.assertTrue(bytesRing.isOne(one));
        Assert.assertFalse(bytesRing.isZero(one));
    }

    @Test
    public void testCreateRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        // create random
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            byte[] randomElement = bytesRing.createRandom(SECURE_RANDOM);
            Assert.assertTrue(bytesRing.validateElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                byte[] randomElement = bytesRing.createRandom(seed);
                Assert.assertTrue(bytesRing.validateElement(randomElement));
                return randomElement;
            })
            .map(ByteBuffer::wrap)
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
            byte[] randomNonZeroElement = bytesRing.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertTrue(bytesRing.validateElement(randomNonZeroElement));
            Assert.assertTrue(bytesRing.validateNonZeroElement(randomNonZeroElement));
            Assert.assertFalse(bytesRing.isZero(randomNonZeroElement));
        });
        // create non-zero random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                byte[] randomNonZeroElement = bytesRing.createNonZeroRandom(seed);
                Assert.assertTrue(bytesRing.validateElement(randomNonZeroElement));
                Assert.assertTrue(bytesRing.validateNonZeroElement(randomNonZeroElement));
                Assert.assertFalse(bytesRing.isZero(randomNonZeroElement));
                return randomNonZeroElement;
            })
            .map(ByteBuffer::wrap)
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
            byte[] randomElement = bytesRing.createRangeRandom(SECURE_RANDOM);
            Assert.assertTrue(bytesRing.validateElement(randomElement));
            Assert.assertTrue(bytesRing.validateRangeElement(randomElement));
        });
        // create random with seed
        long randomNum = IntStream.range(0, MAX_RANDOM)
            .mapToObj(index -> {
                byte[] randomRangeElement = bytesRing.createRangeRandom(seed);
                Assert.assertTrue(bytesRing.validateElement(randomRangeElement));
                Assert.assertTrue(bytesRing.validateRangeElement(randomRangeElement));
                return randomRangeElement;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, randomNum);
    }

    @Test
    public void testConstantAddNegSub() {
        byte[] zero = bytesRing.createZero();
        byte[] p;
        byte[] copyP;
        byte[] t;
        // 0 + 0 = 0
        p = bytesRing.createZero();
        t = bytesRing.add(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.addi(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
        // -0 = 0
        p = bytesRing.createZero();
        t = bytesRing.neg(p);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.negi(copyP);
        Assert.assertArrayEquals(zero, copyP);
        // 0 - 0 = 0
        p = bytesRing.createZero();
        t = bytesRing.sub(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.subi(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
    }

    @Test
    public void testRandomAddNegSub() {
        byte[] zero = bytesRing.createZero();
        byte[] r;
        byte[] copyR;
        byte[] s;
        byte[] t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            r = bytesRing.createRandom(SECURE_RANDOM);
            s = bytesRing.createRandom(SECURE_RANDOM);
            // r + 0 = r
            t = bytesRing.add(r, zero);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.addi(copyR, zero);
            Assert.assertArrayEquals(r, copyR);
            // r - 0 = r
            t = bytesRing.sub(r, zero);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.subi(copyR, zero);
            Assert.assertArrayEquals(r, copyR);
            // -(-r) = r
            t = bytesRing.neg(bytesRing.neg(r));
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.negi(copyR);
            bytesRing.negi(copyR);
            Assert.assertArrayEquals(r, copyR);
            // r + s - s = r
            t = bytesRing.sub(bytesRing.add(r, s), s);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.addi(copyR, s);
            bytesRing.subi(copyR, s);
            Assert.assertArrayEquals(r, copyR);
            // r - s + s = r
            t = bytesRing.add(bytesRing.sub(r, s), s);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.subi(copyR, s);
            bytesRing.addi(copyR, s);
            Assert.assertArrayEquals(r, copyR);
            // (-r) + r = 0
            t = bytesRing.add(r, bytesRing.neg(r));
            Assert.assertArrayEquals(zero, t);
            copyR = BytesUtils.clone(r);
            bytesRing.negi(copyR);
            bytesRing.addi(copyR, r);
            Assert.assertArrayEquals(zero, copyR);
            // r - r = 0
            t = bytesRing.sub(r, r);
            Assert.assertArrayEquals(zero, t);
            copyR = BytesUtils.clone(r);
            bytesRing.subi(copyR, r);
            Assert.assertArrayEquals(zero, copyR);
        }
    }

    @Test
    public void testConstantMul() {
        byte[] zero = bytesRing.createZero();
        byte[] one = bytesRing.createOne();
        byte[] p;
        byte[] copyP;
        byte[] t;
        // 0 * 0 = 0
        p = bytesRing.createZero();
        t = bytesRing.mul(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
        // 0 * 1 = 0
        p = bytesRing.createZero();
        t = bytesRing.mul(p, one);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, one);
        Assert.assertArrayEquals(zero, copyP);
        // 1 * 0 = 0
        p = bytesRing.createOne();
        t = bytesRing.mul(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
        // 1 * 1 = 1
        p = bytesRing.createOne();
        t = bytesRing.mul(p, one);
        Assert.assertArrayEquals(one, t);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, one);
        Assert.assertArrayEquals(one, copyP);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, copyP);
        Assert.assertArrayEquals(one, copyP);
    }

    @Test
    public void testRandomMul() {
        byte[] zero = bytesRing.createZero();
        byte[] one = bytesRing.createOne();
        byte[] r;
        byte[] copyR;
        byte[] t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r * 0 = 0
            r = bytesRing.createRandom(SECURE_RANDOM);
            t = bytesRing.mul(r, zero);
            Assert.assertArrayEquals(zero, t);
            copyR = BytesUtils.clone(r);
            bytesRing.muli(copyR, zero);
            Assert.assertArrayEquals(zero, copyR);
            // r * 1 = r
            r = bytesRing.createNonZeroRandom(SECURE_RANDOM);
            t = bytesRing.mul(r, one);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.muli(copyR, one);
            Assert.assertArrayEquals(r, copyR);
        }
    }

    @Test
    public void testAddParallel() {
        byte[] p = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        byte[] q = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                return bytesRing.add(copyP, copyQ);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);

        long addiCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                bytesRing.addi(copyP, copyQ);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, addiCount);
    }

    @Test
    public void testNegParallel() {
        byte[] p = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                return bytesRing.neg(copyP);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);

        long addiCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                bytesRing.negi(copyP);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, addiCount);
    }

    @Test
    public void testSubParallel() {
        byte[] p = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        byte[] q = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        long subCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                return bytesRing.sub(copyP, copyQ);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, subCount);

        long subiCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                bytesRing.subi(copyP, copyQ);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, subiCount);
    }

    @Test
    public void testMulParallel() {
        byte[] p = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        byte[] q = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                return bytesRing.mul(copyP, copyQ);
            }).map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, mulCount);

        long muliCount = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                bytesRing.muli(copyP, copyQ);
                return copyP;
            }).map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, muliCount);
    }
}
