package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory.EccType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 椭圆曲线测试。
 *
 * @author Weiran Liu
 * @date 2021/12/13
 */
@RunWith(Parameterized.class)
public class EccTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 100;
    /**
     * 每个数组的最大长度
     */
    private static final int MAX_ARRAY_LENGTH = 40;
    /**
     * 并发数量
     */
    private static final int PARALLEL_NUM = 400;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // SEC_P256_K1_OPENSSL
        configurations.add(new Object[]{EccType.SEC_P256_K1_OPENSSL.name(), EccType.SEC_P256_K1_OPENSSL,});
        // SEC_P256_K1_BC
        configurations.add(new Object[]{EccType.SEC_P256_K1_BC.name(), EccType.SEC_P256_K1_BC,});
        // SEC_P256_R1_OPENSSL
        configurations.add(new Object[]{EccType.SEC_P256_R1_OPENSSL.name(), EccType.SEC_P256_R1_OPENSSL,});
        // SEC_P256_R1_BC
        configurations.add(new Object[]{EccType.SEC_P256_R1_BC.name(), EccType.SEC_P256_R1_BC,});
        // SM2_P256_V1_OPENSSL
        configurations.add(new Object[]{EccType.SM2_P256_V1_OPENSSL.name(), EccType.SM2_P256_V1_OPENSSL,});
        // SM2_P256_V1_BC
        configurations.add(new Object[]{EccType.SM2_P256_V1_BC.name(), EccType.SM2_P256_V1_BC,});
        // CURVE_25519_BC
        configurations.add(new Object[]{EccType.CURVE25519_BC.name(), EccType.CURVE25519_BC,});
        // ED_25519_BC
        configurations.add(new Object[]{EccType.ED25519_BC.name(), EccType.ED25519_BC,});

        return configurations;
    }

    /**
     * 待测试的椭圆曲线类型
     */
    private final EccType eccType;

    public EccTest(String name, EccType eccType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.eccType = eccType;
    }

    @Test
    public void testIllegalInputs() {
        Ecc ecc = EccFactory.createInstance(eccType);
        // hash data with length = 0
        Assert.assertThrows(AssertionError.class, () -> ecc.hashToCurve(new byte[0]));
        // inner product with 0 element
        Assert.assertThrows(AssertionError.class, () -> ecc.innerProduct(new ECPoint[0], new boolean[0]));
        // inner product with different length
        Assert.assertThrows(AssertionError.class, () -> {
            ECPoint[] points = new ECPoint[]{ecc.getG()};
            boolean[] binary = new boolean[2];
            ecc.innerProduct(points, binary);
        });
    }

    @Test
    public void testType() {
        Ecc ecc = EccFactory.createInstance(eccType);
        Assert.assertEquals(eccType, ecc.getEccType());
    }

    @Test
    public void testHashToCurve() {
        testHashToCurve(1);
        testHashToCurve(CommonConstants.STATS_BYTE_LENGTH);
        testHashToCurve(CommonConstants.BLOCK_BYTE_LENGTH);
        testHashToCurve(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testHashToCurve(int messageByteLength) {
        Ecc ecc = EccFactory.createInstance(eccType);
        byte[] message = new byte[messageByteLength];
        ECPoint hash1 = ecc.hashToCurve(message);
        Assert.assertTrue(hash1.isValid());
        ECPoint hash2 = ecc.hashToCurve(message);
        Assert.assertEquals(hash1, hash2);
    }

    @Test
    public void testRandomHashToCurve() {
        Ecc ecc = EccFactory.createInstance(eccType);
        Set<ECPoint> hashPointSet = new HashSet<>();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(message);
            hashPointSet.add(ecc.hashToCurve(message));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, hashPointSet.size());
    }

    @Test
    public void testSingleMultiply() {
        Ecc ecc = EccFactory.createInstance(eccType);
        ECPoint g = ecc.getG();
        // 生成一个未归一化（normalized）的椭圆曲线点h
        ECPoint h = g.multiply(ecc.randomZn(SECURE_RANDOM));
        Assert.assertNotEquals(BigInteger.ONE, h.getZCoords()[0].toBigInteger());
        // 生成r和r^{-1}
        BigInteger r = ecc.randomZn(SECURE_RANDOM);
        BigInteger rInv = r.modInverse(ecc.getN());
        // g^r
        ECPoint gr = ecc.multiply(g, r);
        ECPoint grInv = ecc.multiply(gr, rInv);
        Assert.assertEquals(g, grInv);
        // h^r
        ECPoint hr = ecc.multiply(h, r);
        ECPoint hrInv = ecc.multiply(hr, rInv);
        Assert.assertEquals(h, hrInv);
    }

    @Test
    public void testMultiply() {
        Ecc ecc = EccFactory.createInstance(eccType);
        ECPoint g = ecc.getG();
        // 生成一个未归一化（normalized）的椭圆曲线点h
        ECPoint h = g.multiply(ecc.randomZn(SECURE_RANDOM));
        Assert.assertNotEquals(BigInteger.ONE, h.getZCoords()[0].toBigInteger());
        // 生成r和r^{-1}
        BigInteger[] rs = IntStream.range(0, MAX_ARRAY_LENGTH)
            .mapToObj(index -> ecc.randomZn(SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] rsInv = Arrays.stream(rs)
            .map(r -> r.modInverse(ecc.getN()))
            .toArray(BigInteger[]::new);
        // g^r
        ECPoint[] gs = IntStream.range(0, MAX_ARRAY_LENGTH)
            .mapToObj(index -> g)
            .toArray(ECPoint[]::new);
        ECPoint[] grs = Arrays.stream(rs)
            .map(r -> ecc.multiply(g, r))
            .toArray(ECPoint[]::new);
        ECPoint[] grsInv = IntStream.range(0, MAX_ARRAY_LENGTH)
            .mapToObj(index -> ecc.multiply(grs[index], rsInv[index]))
            .toArray(ECPoint[]::new);
        Assert.assertArrayEquals(gs, grsInv);
        // h^r
        ECPoint[] hs = IntStream.range(0, MAX_ARRAY_LENGTH)
            .mapToObj(index -> h)
            .toArray(ECPoint[]::new);
        ECPoint[] hrs = Arrays.stream(rs)
            .map(r -> ecc.multiply(h, r))
            .toArray(ECPoint[]::new);
        ECPoint[] hrsInv = IntStream.range(0, MAX_ARRAY_LENGTH)
            .mapToObj(index -> ecc.multiply(hrs[index], rsInv[index]))
            .toArray(ECPoint[]::new);
        Assert.assertArrayEquals(hs, hrsInv);
    }

    @Test
    public void testPrecompute() {
        Ecc ecc = EccFactory.createInstance(eccType);
        ECPoint g = ecc.getG();
        // 生成一个未归一化（normalized）的椭圆曲线点h
        ECPoint h = g.multiply(ecc.randomZn(SECURE_RANDOM));
        Assert.assertNotEquals(BigInteger.ONE, h.getZCoords()[0].toBigInteger());
        // 生成r
        BigInteger r = ecc.randomZn(SECURE_RANDOM);
        // 预计算g后计算g^r
        ecc.precompute(g);
        ECPoint grPrecompute = ecc.multiply(g, r);
        // 移除预计算g后再计算g^r
        ecc.destroyPrecompute(g);
        ECPoint gr = ecc.multiply(g, r);
        Assert.assertEquals(gr, grPrecompute);
        // 预计算h后计算h^r
        ecc.precompute(h);
        ECPoint hrPrecompute = ecc.multiply(h, r);
        // 移除预计算h后再计算h^r
        ecc.destroyPrecompute(h);
        ECPoint hr = ecc.multiply(h, r);
        Assert.assertEquals(hr, hrPrecompute);
    }

    @Test
    public void testAddition() {
        testAddition(1);
        testAddition(MAX_ARRAY_LENGTH);
        testAddition(CommonConstants.BLOCK_BIT_LENGTH);
    }

    private void testAddition(int num) {
        Ecc ecc = EccFactory.createInstance(eccType);
        ECPoint g = ecc.getG();
        ECPoint gs = IntStream.range(0, num)
            .mapToObj(index -> g)
            .reduce(ecc::add)
            .orElse(ecc.getInfinity());
        Assert.assertEquals(ecc.multiply(g, BigInteger.valueOf(num)), gs);
    }

    @Test
    public void testEncode() {
        Ecc ecc = EccFactory.createInstance(eccType);
        // 随机生成点
        ECPoint[] hs = IntStream.range(0, MAX_ARRAY_LENGTH)
            .mapToObj(index -> ecc.getG())
            .map(g -> g.multiply(ecc.randomZn(SECURE_RANDOM)))
            .toArray(ECPoint[]::new);
        // 非压缩编码
        byte[][] uncompressedEncodes = Arrays.stream(hs)
            .map(h -> ecc.encode(h, false))
            .toArray(byte[][]::new);
        Arrays.stream(uncompressedEncodes)
            .map(ecc::decode)
            .forEach(h -> Assert.assertTrue(h.isValid()));
        // 压缩编码
        byte[][] compressedEncodes = Arrays.stream(hs)
            .map(h -> ecc.encode(h, true))
            .toArray(byte[][]::new);
        Arrays.stream(compressedEncodes)
            .map(ecc::decode)
            .forEach(h -> Assert.assertTrue(h.isValid()));
    }


    @Test
    public void testInnerProduct() {
        testInnerProduct(1);
        testInnerProduct(MAX_ARRAY_LENGTH);
        testInnerProduct(CommonConstants.BLOCK_BIT_LENGTH);
    }

    private void testInnerProduct(int num) {
        Ecc ecc = EccFactory.createInstance(eccType);
        ECPoint g = ecc.getG();
        ECPoint[] gs = IntStream.range(0, num).mapToObj(index -> g).toArray(ECPoint[]::new);
        boolean[] binary = new boolean[num];
        // 全0加
        Assert.assertEquals(ecc.getInfinity(), ecc.innerProduct(gs, binary));
        // 全1加
        Arrays.fill(binary, true);
        Assert.assertEquals(ecc.multiply(g, BigInteger.valueOf(num)), ecc.innerProduct(gs, binary));
    }

    @Test
    public void testParallel() {
        Ecc ecc = EccFactory.createInstance(eccType);
        // HashToCurve并发测试
        byte[][] messages = IntStream.range(0, PARALLEL_NUM)
            .mapToObj(index -> new byte[CommonConstants.BLOCK_BYTE_LENGTH])
            .toArray(byte[][]::new);
        Set<ECPoint> hashMessageSet = Arrays.stream(messages)
            .parallel()
            .map(ecc::hashToCurve)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashMessageSet.size());
        // RandomPoint并发测试
        Set<ECPoint> randomPointSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> ecc.randomPoint(SECURE_RANDOM))
            .collect(Collectors.toSet());
        Assert.assertEquals(PARALLEL_NUM, randomPointSet.size());
        // 乘法并发测试
        BigInteger gr = BigIntegerUtils.randomPositive(ecc.getN(), SECURE_RANDOM);
        ECPoint h = ecc.multiply(ecc.getG(), gr);
        BigInteger r = BigIntegerUtils.randomPositive(ecc.getN(), SECURE_RANDOM);
        Set<ECPoint> multiplySet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> ecc.multiply(h, r))
            .collect(Collectors.toSet());
        Assert.assertEquals(1, multiplySet.size());
        // 预计算
        ecc.precompute(h);
        // 预计算乘法并发测试
        Set<ECPoint> precomputeMultiplySet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> ecc.multiply(h, r))
            .collect(Collectors.toSet());
        Assert.assertEquals(1, precomputeMultiplySet.size());
        ecc.destroyPrecompute(h);
    }
}
