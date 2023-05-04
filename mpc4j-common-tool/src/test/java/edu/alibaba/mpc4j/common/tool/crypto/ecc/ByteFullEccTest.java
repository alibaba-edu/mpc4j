package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 全功能字节椭圆曲线测试。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
@RunWith(Parameterized.class)
public class ByteFullEccTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 100;
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

        // FourQ
        configurations.add(new Object[]{ByteEccType.FOUR_Q.name(), ByteEccType.FOUR_Q,});
        // ED25519_SODIUM
        configurations.add(new Object[]{ByteEccType.ED25519_SODIUM.name(), ByteEccType.ED25519_SODIUM,});
        // ED25519_BC
        configurations.add(new Object[]{ByteEccType.ED25519_BC.name(), ByteEccType.ED25519_BC,});
        // ED25519_CAFE
        configurations.add(new Object[]{ByteEccType.ED25519_CAFE.name(), ByteEccType.ED25519_CAFE,});
        // RISTRETTO_CAFE
        configurations.add(new Object[]{ByteEccType.RISTRETTO_CAFE.name(), ByteEccType.RISTRETTO_CAFE,});

        return configurations;
    }

    /**
     * 待测试的字节椭圆曲线类型
     */
    private final ByteEccType byteEccType;

    public ByteFullEccTest(String name, ByteEccType byteEccType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.byteEccType = byteEccType;
    }
    @Test
    public void testType() {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        Assert.assertEquals(byteEccType, byteFullEcc.getByteEccType());
    }

    @Test
    public void testHashToCurve() {
        testHashToCurve(1);
        testHashToCurve(CommonConstants.STATS_BYTE_LENGTH);
        testHashToCurve(CommonConstants.BLOCK_BYTE_LENGTH);
        testHashToCurve(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testHashToCurve(int messageByteLength) {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        byte[] message = new byte[messageByteLength];
        byte[] hash1 = byteFullEcc.hashToCurve(message);
        Assert.assertTrue(byteFullEcc.isValidPoint(hash1));
        byte[] hash2 = byteFullEcc.hashToCurve(message);
        Assert.assertArrayEquals(hash1, hash2);
    }

    @Test
    public void testRandomHashToCurve() {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        Set<ByteBuffer> pointSet = new HashSet<>();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(message);
            byte[] p = byteFullEcc.hashToCurve(message);
            Assert.assertTrue(byteFullEcc.isValidPoint(p));
            pointSet.add(ByteBuffer.wrap(byteFullEcc.hashToCurve(message)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, pointSet.size());
    }

    @Test
    public void testMul() {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        byte[] g = byteFullEcc.getG();
        // 生成一个椭圆曲线点h
        byte[] h = byteFullEcc.randomPoint(SECURE_RANDOM);
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 生成r和r^{-1}
            BigInteger r = byteFullEcc.randomZn(SECURE_RANDOM);
            BigInteger rInv = r.modInverse(byteFullEcc.getN());
            // g^r
            byte[] gr = byteFullEcc.mul(g, r);
            byte[] grInv = byteFullEcc.mul(gr, rInv);
            Assert.assertArrayEquals(g, grInv);
            // h^r
            byte[] hr = byteFullEcc.mul(h, r);
            byte[] hrInv = byteFullEcc.mul(hr, rInv);
            Assert.assertArrayEquals(h, hrInv);
        }
    }

    @Test
    public void testBaseMul() {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        byte[] g = byteFullEcc.getG();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 生成r和r^{-1}
            BigInteger r = byteFullEcc.randomZn(SECURE_RANDOM);
            BigInteger rInv = r.modInverse(byteFullEcc.getN());
            // g^r
            byte[] gr = byteFullEcc.mul(g, r);
            // 应用底数幂乘计算
            byte[] baseGr = byteFullEcc.baseMul(r);
            Assert.assertArrayEquals(gr, baseGr);
            byte[] baseGrInv = byteFullEcc.mul(gr, rInv);
            Assert.assertArrayEquals(g, baseGrInv);
        }
    }

    @Test
    public void testAddSub() {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        byte[] g = byteFullEcc.getG();
        byte[] expect = byteFullEcc.baseMul(BigInteger.valueOf(MAX_RANDOM_ROUND));
        // 连续求和
        byte[] actual = byteFullEcc.getInfinity();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            actual = byteFullEcc.add(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        // 连续内部求和
        actual = byteFullEcc.getInfinity();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byteFullEcc.addi(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        byte[] positive = BytesUtils.clone(actual);

        BigInteger n = byteFullEcc.getN();
        expect = byteFullEcc.baseMul(BigInteger.valueOf(MAX_RANDOM_ROUND).negate().mod(n));
        actual = byteFullEcc.getInfinity();
        // 连续求差
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            actual = byteFullEcc.sub(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        // 连续内部求差
        actual = byteFullEcc.getInfinity();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byteFullEcc.subi(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        byte[] negative = BytesUtils.clone(actual);

        // 验证求逆
        expect = byteFullEcc.neg(negative);
        Assert.assertArrayEquals(positive, expect);
        expect = byteFullEcc.neg(positive);
        Assert.assertArrayEquals(negative, expect);
        // 验证内部求逆
        expect = BytesUtils.clone(negative);
        byteFullEcc.negi(expect);
        Assert.assertArrayEquals(positive, expect);
        expect = BytesUtils.clone(positive);
        byteFullEcc.negi(expect);
        Assert.assertArrayEquals(negative, expect);
    }

    @Test
    public void testParallel() {
        ByteFullEcc byteFullEcc = ByteEccFactory.createFullInstance(byteEccType);
        // HashToCurve并发测试
        byte[][] messages = IntStream.range(0, PARALLEL_NUM)
            .mapToObj(index -> new byte[CommonConstants.BLOCK_BYTE_LENGTH])
            .toArray(byte[][]::new);
        Set<ByteBuffer> hashMessageSet = Arrays.stream(messages)
            .parallel()
            .map(byteFullEcc::hashToCurve)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashMessageSet.size());
        // RandomPoint并发测试
        Set<ByteBuffer> randomPointSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteFullEcc.randomPoint(SECURE_RANDOM))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(PARALLEL_NUM, randomPointSet.size());
        // 运算并发测试
        byte[] p = byteFullEcc.randomPoint(SECURE_RANDOM);
        byte[] q = byteFullEcc.randomPoint(SECURE_RANDOM);
        // 加法并发测试
        Set<ByteBuffer> addSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteFullEcc.add(p, q))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, addSet.size());
        // 减法并发测试
        Set<ByteBuffer> subSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteFullEcc.sub(p, q))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, subSet.size());
        // 乘法并发测试
        BigInteger r = byteFullEcc.randomZn(SECURE_RANDOM);
        Set<ByteBuffer> mulSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteFullEcc.mul(p, r))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, mulSet.size());
    }
}
