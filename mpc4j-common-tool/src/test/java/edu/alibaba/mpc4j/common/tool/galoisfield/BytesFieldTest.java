package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
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
 * BytesField tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class BytesFieldTest {
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
     * the BytesField instance
     */
    private final BytesField bytesField;
    /**
     * the element byte length
     */
    private final int elementByteLength;

    public BytesFieldTest(String name, BytesField bytesField) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.bytesField = bytesField;
        elementByteLength = bytesField.getElementByteLength();
    }

    @Test
    public void testIllegalInputs() {
        // try operating p and q when p is invalid
        final byte[] invalidP = new byte[elementByteLength - 1];
        SECURE_RANDOM.nextBytes(invalidP);
        final byte[] q = bytesField.createNonZeroRandom(SECURE_RANDOM);
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> bytesField.div(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesField.divi(invalidP, q));

        // try operating p and q when q is invalid
        final byte[] p = bytesField.createNonZeroRandom(SECURE_RANDOM);
        final byte[] invalidQ = new byte[elementByteLength - 1];
        SECURE_RANDOM.nextBytes(invalidQ);
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> bytesField.div(p, invalidQ));
        Assert.assertThrows(AssertionError.class, () -> bytesField.divi(p, invalidQ));

        // try operating p when p is invalid
        // try inverting p
        Assert.assertThrows(AssertionError.class, () -> bytesField.inv(invalidP));
        Assert.assertThrows(AssertionError.class, () -> bytesField.invi(invalidP));
    }

    @Test
    public void testConstantMulInvDiv() {
        byte[] zero = bytesField.createZero();
        byte[] one = bytesField.createOne();
        byte[] p;
        byte[] copyP;
        byte[] t;
        // 0 / 1 = 0
        p = bytesField.createZero();
        t = bytesField.div(p, one);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesField.divi(copyP, one);
        Assert.assertArrayEquals(zero, copyP);
        // 1^{-1} = 1
        p = bytesField.createOne();
        t = bytesField.inv(p);
        Assert.assertArrayEquals(one, t);
        copyP = BytesUtils.clone(p);
        bytesField.invi(copyP);
        Assert.assertArrayEquals(one, copyP);
        // 1 / 1 = 1
        p = bytesField.createOne();
        t = bytesField.div(p, one);
        Assert.assertArrayEquals(one, t);
        copyP = BytesUtils.clone(p);
        bytesField.divi(copyP, one);
        Assert.assertArrayEquals(one, copyP);
    }

    @Test
    public void testRandomMulInvDiv() {
        byte[] one = bytesField.createOne();
        byte[] r;
        byte[] copyR;
        byte[] t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r / 1 = r
            r = bytesField.createNonZeroRandom(SECURE_RANDOM);
            t = bytesField.div(r, one);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesField.divi(copyR, one);
            Assert.assertArrayEquals(r, copyR);
            // r / r = 1
            r = bytesField.createNonZeroRandom(SECURE_RANDOM);
            t = bytesField.div(r, r);
            Assert.assertArrayEquals(one, t);
            copyR = BytesUtils.clone(r);
            bytesField.divi(copyR, r);
            Assert.assertArrayEquals(one, copyR);
            copyR = BytesUtils.clone(r);
            bytesField.divi(copyR, copyR);
            Assert.assertArrayEquals(one, copyR);
        }
    }

    @Test
    public void testInvParallel() {
        byte[] p = bytesField.createNonZeroRandom(SECURE_RANDOM);
        long invCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                return bytesField.inv(copyP);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, invCount);

        long inviCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                bytesField.invi(copyP);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, inviCount);
    }

    @Test
    public void testDivParallel() {
        byte[] p = bytesField.createNonZeroRandom(SECURE_RANDOM);
        byte[] q = bytesField.createNonZeroRandom(SECURE_RANDOM);
        long divCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                return bytesField.div(copyP, copyQ);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, divCount);

        long diviCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                byte[] copyQ = BytesUtils.clone(q);
                bytesField.divi(copyP, copyQ);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, diviCount);
    }
}
