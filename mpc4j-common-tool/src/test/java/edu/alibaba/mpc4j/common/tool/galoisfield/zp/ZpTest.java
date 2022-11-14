package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
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
 * Zp功能测试。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
@RunWith(Parameterized.class)
public class ZpTest {
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
        // JDK
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 1)", ZpType.JDK, 1});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 2)", ZpType.JDK, 2});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 3)", ZpType.JDK, 3});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 4)", ZpType.JDK, 4});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 39)", ZpType.JDK, 39});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 40)", ZpType.JDK, 40});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 41)", ZpType.JDK, 41});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 62)", ZpType.JDK, 62});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 63)", ZpType.JDK, 63});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 64)", ZpType.JDK, 64});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 65)", ZpType.JDK, 65});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 127)", ZpType.JDK, 127});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 128)", ZpType.JDK, 128});
        configurations.add(new Object[]{ZpType.JDK.name() + " (l = 129)", ZpType.JDK, 129});

        return configurations;
    }

    /**
     * Zp运算类型
     */
    private final ZpType type;
    /**
     * Zp有限域
     */
    private final Zp zp;
    /**
     * 用于测试并发性能的常数
     */
    private final BigInteger constant;

    public ZpTest(String name, ZpType type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        zp = ZpFactory.createInstance(EnvType.STANDARD, type, l);
        constant = BigInteger.ONE.shiftLeft(l);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, zp.getZpType());
    }

    @Test
    public void testBitLength() {
        int primeBitLength = zp.getPrimeBitLength();
        int l = zp.getL();
        Assert.assertEquals(primeBitLength, l + 1);
    }

    @Test
    public void testByteLength() {
        int primeByteLength = zp.getPrimeByteLength();
        int byteL = zp.getByteL();
        if (zp.getL() % Byte.SIZE == 0) {
            // 如果l刚好可以被Byte.SIZE整除，则质数字节长度会更大一点
            Assert.assertEquals(primeByteLength, byteL + 1);
        } else {
            Assert.assertEquals(primeByteLength, byteL);
        }
    }

    @Test
    public void testModulus() {
        // 0 mod p = 0
        Assert.assertEquals(BigInteger.ZERO, zp.module(BigInteger.ZERO));
        // 1 mod p = 1
        Assert.assertEquals(BigInteger.ONE, zp.module(BigInteger.ONE));
        // p + 0 mod p = 0
        Assert.assertEquals(BigInteger.ZERO, zp.module(zp.getPrime()));
        // p + 1 mod p = 1
        Assert.assertEquals(BigInteger.ONE, zp.module(zp.getPrime().add(BigInteger.ONE)));
        // -1 mod p = p - 1
        Assert.assertEquals(zp.getPrime().subtract(BigInteger.ONE), zp.module(BigInteger.ONE.negate()));
        // 1 - p mod p = 1
        Assert.assertEquals(BigInteger.ONE, zp.module(BigInteger.ONE.subtract(zp.getPrime())));
    }

    @Test
    public void testAdd() {
        // 0 + 0 = 0
        BigInteger zero = zp.createZero();
        Assert.assertEquals(zero, zp.add(zero, zero));
        // 0 - 0 = 0
        Assert.assertEquals(zero, zp.sub(zero, zero));
        // 0 + (-0) = 0
        Assert.assertEquals(zero, zp.add(zero, zp.neg(zero)));

        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            BigInteger a = zp.createNonZeroRandom(SECURE_RANDOM);
            // a + (-a) = 0
            Assert.assertEquals(zero, zp.add(a, zp.neg(a)));
            // a - a = 0
            Assert.assertEquals(zero, zp.sub(a, a));
        }
    }

    @Test
    public void testMul() {
        // 0 * 0 = 0
        BigInteger zero = zp.createZero();
        Assert.assertEquals(zero, zp.mul(zero, zero));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            BigInteger a = zp.createNonZeroRandom(SECURE_RANDOM);
            // 0 * a = 0
            Assert.assertEquals(zero, zp.mul(zero, a));
            // a * 0 = 0
            Assert.assertEquals(zero, zp.mul(a, zero));
        }
        // 1 * 1 = 1
        BigInteger one = zp.createOne();
        Assert.assertEquals(one, zp.mul(one, one));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            BigInteger a = zp.createNonZeroRandom(SECURE_RANDOM);
            // 1 * a = a
            Assert.assertEquals(a, zp.mul(one, a));
            // a * 1 = a
            Assert.assertEquals(a, zp.mul(a, one));
            // a * (1 / a) = 1
            Assert.assertEquals(one, zp.mul(a, zp.inv(a)));
            // a / a = 1
            Assert.assertEquals(one, zp.div(a, a));
        }
    }

    @Test
    public void testModPow() {
        // 0^0 = 1
        BigInteger zero = zp.createZero();
        BigInteger one = zp.createOne();
        Assert.assertEquals(one, zp.mulPow(zero, zero));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // 0^a = 0
            BigInteger a = zp.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertEquals(zero, zp.mulPow(zero, a));
        }
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // a^0 = 1
            BigInteger a = zp.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertEquals(one, zp.mulPow(a, zero));
        }
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // (a^b)^(-1) = (a^(-1))^b
            BigInteger a = zp.createNonZeroRandom(SECURE_RANDOM);
            BigInteger b = zp.createNonZeroRandom(SECURE_RANDOM);
            Assert.assertEquals(zp.inv(zp.mulPow(a, b)), zp.mulPow(zp.inv(a), b));
        }
    }

    @Test
    public void testAddParallel() {
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.add(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);

        long subCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.sub(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, subCount);

        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.neg(constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);
    }

    @Test
    public void testMulParallel() {
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.mul(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulCount);

        long divCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.div(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, divCount);

        long invCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.inv(constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, invCount);
    }

    @Test
    public void testModPowParallel() {
        long mulPowCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> zp.mulPow(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulPowCount);
    }
}
