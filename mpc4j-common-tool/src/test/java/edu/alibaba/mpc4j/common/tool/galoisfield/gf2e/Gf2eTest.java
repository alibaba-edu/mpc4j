package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * GF(2^l)功能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
@RunWith(Parameterized.class)
public class Gf2eTest {
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * 随机测试数量
     */
    private static final int MAX_RANDOM_NUM = 40;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // NTL
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 1)", Gf2eType.NTL, 1});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 2)", Gf2eType.NTL, 2});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 3)", Gf2eType.NTL, 3});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 4)", Gf2eType.NTL, 4});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 39)", Gf2eType.NTL, 39});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 40)", Gf2eType.NTL, 40});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 41)", Gf2eType.NTL, 41});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 128)", Gf2eType.NTL, 128});
        configurations.add(new Object[] {Gf2eType.NTL.name() + " (l = 256)", Gf2eType.NTL, 256});
        // Rings
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 1)", Gf2eType.RINGS, 1});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 2)", Gf2eType.RINGS, 2});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 3)", Gf2eType.RINGS, 3});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 4)", Gf2eType.RINGS, 4});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 39)", Gf2eType.RINGS, 39});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 40)", Gf2eType.RINGS, 40});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 41)", Gf2eType.RINGS, 41});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 128)", Gf2eType.RINGS, 128});
        configurations.add(new Object[] {Gf2eType.RINGS.name() + " (l = 256)", Gf2eType.RINGS, 256});

        return configurations;
    }

    /**
     * GF(2^l)运算类型
     */
    private final Gf2eType type;
    /**
     * 有限域字节长度
     */
    private final int byteL;
    /**
     * GF(2^l)运算
     */
    private final Gf2e gf2e;
    /**
     * 用于测试并发性能的常数
     */
    private final byte[] constant;

    public Gf2eTest(String name, Gf2eType type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        gf2e = Gf2eFactory.createInstance(type, l);
        byteL = gf2e.getByteL();
        constant = new byte[gf2e.getByteL()];
        Arrays.fill(constant, (byte)0xFF);
        BytesUtils.reduceByteArray(constant, l);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, gf2e.getGf2eType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试对错误长度的a做运算
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2e.add(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2e.addi(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2e.mul(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2e.muli(a, b);
            throw new IllegalStateException("ERROR: successfully compute a * b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        // 尝试对错误长度的b做运算
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2e.add(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length b");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2e.addi(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length b");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2e.mul(a, b);
            throw new IllegalStateException("ERROR: successfully compute a * b for wrong-length b");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2e.muli(a, b);
            throw new IllegalStateException("ERROR: successfully compute a * b for wrong-length b");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testAdd() {
        byte[] zero = gf2e.createZero();
        // 0 + 0 = 0
        Assert.assertArrayEquals(zero, gf2e.add(zero, zero));
        // 0 - 0 = 0
        Assert.assertArrayEquals(zero, gf2e.sub(zero, zero));
        // 0 + (-0) = 0
        Assert.assertArrayEquals(zero, gf2e.add(zero, gf2e.neg(zero)));

        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            byte[] a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            // a + (-a) = 0
            Assert.assertArrayEquals(zero, gf2e.add(a, gf2e.neg(a)));
            // a - a = 0
            Assert.assertArrayEquals(zero, gf2e.sub(a, a));
        }
    }

    @Test
    public void testAddi() {
        byte[] zero = gf2e.createZero();
        // 0 + 0 = 0
        byte[] a = gf2e.createZero();
        gf2e.addi(a, gf2e.createZero());
        Assert.assertArrayEquals(zero, a);
        // 0 - 0 = 0
        a = gf2e.createZero();
        gf2e.subi(a, gf2e.createZero());
        Assert.assertArrayEquals(zero, a);
        // 0 + (-0) = 0
        a = gf2e.createZero();
        gf2e.addi(a, gf2e.neg(gf2e.createZero()));
        Assert.assertArrayEquals(zero, a);

        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            // a + (-a) = 0
            gf2e.addi(a, gf2e.neg(a));
            Assert.assertArrayEquals(zero, a);
            // a - a = 0
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            gf2e.subi(a, a);
            Assert.assertArrayEquals(zero, a);
        }
    }

    @Test
    public void testMul() {
        byte[] zero = gf2e.createZero();
        // 0 * 0 = 0
        byte[] a = gf2e.createZero();
        byte[] b = gf2e.createZero();
        Assert.assertArrayEquals(zero, gf2e.mul(a, b));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // 0 * a = 0
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertArrayEquals(zero, gf2e.mul(zero, a));
            // a * 0 = 0
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertArrayEquals(zero, gf2e.mul(a, zero));
        }
        byte[] one = gf2e.createOne();
        // 1 * 1 = 1
        a = gf2e.createOne();
        b = gf2e.createOne();
        Assert.assertArrayEquals(one, gf2e.mul(a, b));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // 1 * a = a
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertArrayEquals(a, gf2e.mul(one, a));
            // a * 1 = a
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertArrayEquals(a, gf2e.mul(a, one));
            // a * (1 / a) = 1
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertArrayEquals(one, gf2e.mul(a, gf2e.inv(a)));
            // a / a = 1
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertArrayEquals(one, gf2e.div(a, a));
        }
        if (gf2e.getL() > 2) {
            // x * x = x^2
            a = new byte[byteL];
            a[a.length - 1] = 0x02;
            b = new byte[byteL];
            b[a.length - 1] = 0x02;
            byte[] truth = new byte[byteL];
            truth[a.length - 1] = 0x04;
            Assert.assertArrayEquals(truth, gf2e.mul(a, b));
        }
        if (gf2e.getL() > 4) {
            // x^2 * x^2 = x^4
            a = new byte[byteL];
            a[a.length - 1] = 0x04;
            b = new byte[byteL];
            b[a.length - 1] = 0x04;
            byte[] truth = new byte[byteL];
            truth[a.length - 1] = 0x10;
            Assert.assertArrayEquals(truth, gf2e.mul(a, b));
        }
    }

    @Test
    public void testMuli() {
        byte[] zero = gf2e.createZero();
        // 0 * 0 = 0
        byte[] a = gf2e.createZero();
        gf2e.muli(a, gf2e.createZero());
        Assert.assertArrayEquals(zero, a);
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // 0 * a = 0
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            gf2e.muli(a, zero);
            Assert.assertArrayEquals(zero, a);
        }
        byte[] one = gf2e.createOne();
        // 1 * 1 = 1
        a = gf2e.createOne();
        gf2e.muli(a, gf2e.createOne());
        Assert.assertArrayEquals(one, a);
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // 1 * a = a
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            byte[] truth = BytesUtils.clone(a);
            gf2e.muli(a, one);
            Assert.assertArrayEquals(truth, a);
            // a * (1 / a) = 1
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            byte[] copyA = BytesUtils.clone(a);
            gf2e.invi(a);
            gf2e.muli(a, copyA);
            Assert.assertArrayEquals(one, a);
            // a / a = 1
            a = gf2e.createNonZeroRandom(SECURE_RANDOM);
            gf2e.divi(a, a);
            Assert.assertArrayEquals(one, a);
        }
        if (gf2e.getL() > 2) {
            // x * x = x^2
            a = new byte[byteL];
            a[a.length - 1] = 0x02;
            byte[] truth = new byte[byteL];
            truth[a.length - 1] = 0x04;
            gf2e.muli(a, a);
            Assert.assertArrayEquals(truth, a);
        }
        if (gf2e.getL() > 4) {
            // x^2 * x^2 = x^4
            a = new byte[byteL];
            a[a.length - 1] = 0x04;
            byte[] truth = new byte[byteL];
            truth[a.length - 1] = 0x10;
            gf2e.muli(a, a);
            Assert.assertArrayEquals(truth, a);
        }
    }

    @Test
    public void testAddParallel() {
        byte[] constant = new byte[gf2e.getByteL()];
        Arrays.fill(constant, (byte)0xFF);
        BytesUtils.reduceByteArray(constant, gf2e.getL());

        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                return gf2e.add(a, b);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);

        long addiCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                gf2e.addi(a, b);
                return a;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, addiCount);

        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                return gf2e.neg(a);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);

        long negiCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                gf2e.negi(a);
                return a;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, negiCount);

        long subCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                return gf2e.sub(a, b);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, subCount);

        long subiCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                gf2e.subi(a, b);
                return a;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1L, subiCount);
    }

    @Test
    public void testMulParallel() {
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                return gf2e.mul(a, b);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, mulCount);

        long muliCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                gf2e.muli(a, b);
                return a;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, muliCount);

        long invCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                return gf2e.inv(a);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, invCount);

        long inviCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                gf2e.invi(a);
                return a;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, inviCount);

        long divCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                return gf2e.div(a, b);
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, divCount);

        long diviCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> {
                byte[] a = BytesUtils.clone(constant);
                byte[] b = BytesUtils.clone(constant);
                gf2e.divi(a, b);
                return a;
            })
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(1, diviCount);
    }
}
