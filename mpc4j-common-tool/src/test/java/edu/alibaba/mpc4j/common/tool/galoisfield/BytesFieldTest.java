package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
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
    private static final int PARALLEL_NUM = 100;
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 400;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // GF2E
        for (Gf2eType type : Gf2eType.values()) {
            // add each l
            for (int l : GaloisfieldTestUtils.GF2E_L_ARRAY) {
                if (Gf2eFactory.available(type, l)) {
                    configurations.add(new Object[]{
                        Gf2eType.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")",
                        Gf2eFactory.createInstance(EnvType.STANDARD, type, l),
                    });
                }
            }
        }
        // GF2K
        for (Gf2kType type : Gf2kType.values()) {
            configurations.add(new Object[]{
                Gf2kType.class.getSimpleName() + " (" + type.name() + ")",
                Gf2kFactory.createInstance(EnvType.STANDARD, type),
            });
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
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BytesFieldTest(String name, BytesField bytesField) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.bytesField = bytesField;
        elementByteLength = bytesField.getElementByteLength();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // try operating p and q when p is invalid
        final byte[] invalidP = new byte[elementByteLength - 1];
        secureRandom.nextBytes(invalidP);
        final byte[] q = bytesField.createNonZeroRandom(secureRandom);
        // try dividing
        Assert.assertThrows(AssertionError.class, () -> bytesField.div(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesField.divi(invalidP, q));

        // try operating p and q when q is invalid
        final byte[] p = bytesField.createNonZeroRandom(secureRandom);
        final byte[] invalidQ = new byte[elementByteLength - 1];
        secureRandom.nextBytes(invalidQ);
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
        // 0 * 0 = 0
        p = bytesField.createZero();
        t = bytesField.mul(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesField.muli(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
        // 0 * 1 = 0
        p = bytesField.createZero();
        t = bytesField.mul(p, one);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesField.muli(copyP, one);
        Assert.assertArrayEquals(zero, copyP);
        // 1 * 0 = 0
        p = bytesField.createOne();
        t = bytesField.mul(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesField.muli(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
        // 1 * 1 = 1
        p = bytesField.createOne();
        t = bytesField.mul(p, one);
        Assert.assertArrayEquals(one, t);
        copyP = BytesUtils.clone(p);
        bytesField.muli(copyP, one);
        Assert.assertArrayEquals(one, copyP);
        // 1^{-1} = 1
        p = bytesField.createOne();
        t = bytesField.inv(p);
        Assert.assertArrayEquals(one, t);
        copyP = BytesUtils.clone(p);
        bytesField.invi(copyP);
        Assert.assertArrayEquals(one, copyP);
        // 0 / 1 = 0
        p = bytesField.createZero();
        t = bytesField.div(p, one);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesField.divi(copyP, one);
        Assert.assertArrayEquals(zero, copyP);
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
        byte[] zero = bytesField.createZero();
        byte[] one = bytesField.createOne();
        byte[] p;
        byte[] copyP;
        byte[] q;
        byte[] r;
        for (int index = 0; index < RANDOM_ROUND; index++) {
            // 0 * q = 0
            p = bytesField.createZero();
            q = bytesField.createRandom(secureRandom);
            r = bytesField.mul(p, q);
            Assert.assertArrayEquals(zero, r);
            copyP = BytesUtils.clone(p);
            bytesField.muli(copyP, q);
            Assert.assertArrayEquals(zero, copyP);
            // 1 * q = q
            p = bytesField.createOne();
            q = bytesField.createRandom(secureRandom);
            r = bytesField.mul(p, q);
            Assert.assertArrayEquals(q, r);
            copyP = BytesUtils.clone(p);
            bytesField.muli(copyP, q);
            Assert.assertArrayEquals(q, copyP);
            // p * 0 = 0
            p = bytesField.createRandom(secureRandom);
            q = bytesField.createZero();
            r = bytesField.mul(p, q);
            Assert.assertArrayEquals(zero, r);
            copyP = BytesUtils.clone(p);
            bytesField.muli(copyP, q);
            Assert.assertArrayEquals(zero, copyP);
            // p * 1 = p
            p = bytesField.createRandom(secureRandom);
            q = bytesField.createOne();
            r = bytesField.mul(p, q);
            Assert.assertArrayEquals(p, r);
            copyP = BytesUtils.clone(p);
            bytesField.muli(copyP, q);
            Assert.assertArrayEquals(p, copyP);
            // p / 1 = p
            p = bytesField.createRandom(secureRandom);
            q = bytesField.createOne();
            r = bytesField.div(p, q);
            Assert.assertArrayEquals(p, r);
            copyP = BytesUtils.clone(p);
            bytesField.divi(copyP, q);
            Assert.assertArrayEquals(p, copyP);
            // p / p = 1
            p = bytesField.createNonZeroRandom(secureRandom);
            r = bytesField.div(p, p);
            Assert.assertArrayEquals(one, r);
            copyP = BytesUtils.clone(p);
            bytesField.divi(copyP, p);
            Assert.assertArrayEquals(one, copyP);
            // p * q / q = p
            p = bytesField.createRandom(secureRandom);
            q = bytesField.createNonZeroRandom(secureRandom);
            r = bytesField.mul(p, q);
            r = bytesField.div(r, q);
            Assert.assertArrayEquals(p, r);
            copyP = BytesUtils.clone(p);
            bytesField.muli(copyP, q);
            bytesField.divi(copyP, q);
            Assert.assertArrayEquals(p, copyP);
            // p / q * q = p
            p = bytesField.createRandom(secureRandom);
            q = bytesField.createNonZeroRandom(secureRandom);
            r = bytesField.div(p, q);
            r = bytesField.mul(r, q);
            Assert.assertArrayEquals(p, r);
            copyP = BytesUtils.clone(p);
            bytesField.divi(copyP, q);
            bytesField.muli(copyP, q);
            Assert.assertArrayEquals(p, copyP);
        }
    }

    @Test
    public void testParallelMulDiv() {
        byte[] p = bytesField.createNonZeroRandom(secureRandom);
        byte[] q = bytesField.createNonZeroRandom(secureRandom);
        long mulCount = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> bytesField.mul(p, q))
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, mulCount);
        long muliCount = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                bytesField.muli(copyP, q);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, muliCount);

        long invCount = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> bytesField.inv(p))
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, invCount);
        long inviCount = IntStream.range(0, PARALLEL_NUM)
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

        long divCount = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> bytesField.div(p, q))
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, divCount);
        long diviCount = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> {
                byte[] copyP = BytesUtils.clone(p);
                bytesField.divi(copyP, q);
                return copyP;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, diviCount);
    }
}
