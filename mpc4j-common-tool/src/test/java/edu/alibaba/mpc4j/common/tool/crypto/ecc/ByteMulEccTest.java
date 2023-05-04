package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 乘法字节椭圆曲线测试。
 *
 * @author Weiran Liu
 * @date 2022/9/2
 */
@RunWith(Parameterized.class)
public class ByteMulEccTest {
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
        // X25519_SODIUM
        configurations.add(new Object[]{ByteEccType.X25519_SODIUM.name(), ByteEccType.X25519_SODIUM,});
        // X25519_BC
        configurations.add(new Object[]{ByteEccType.X25519_BC.name(), ByteEccType.X25519_BC,});
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

    public ByteMulEccTest(String name, ByteEccType byteEccType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.byteEccType = byteEccType;
    }

    @Test
    public void testIllegalInputs() {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        // try hash byte[0] to curve.
        Assert.assertThrows(AssertionError.class, () -> byteMulEcc.hashToCurve(new byte[0]));
    }

    @Test
    public void testType() {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        Assert.assertEquals(byteEccType, byteMulEcc.getByteEccType());
    }

    @Test
    public void testHashToCurve() {
        testHashToCurve(1);
        testHashToCurve(CommonConstants.STATS_BYTE_LENGTH);
        testHashToCurve(CommonConstants.BLOCK_BYTE_LENGTH);
        testHashToCurve(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testHashToCurve(int messageByteLength) {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        byte[] message = new byte[messageByteLength];
        byte[] hash1 = byteMulEcc.hashToCurve(message);
        Assert.assertTrue(byteMulEcc.isValidPoint(hash1));
        byte[] hash2 = byteMulEcc.hashToCurve(message);
        Assert.assertArrayEquals(hash1, hash2);
    }

    @Test
    public void testRandomHashToCurve() {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        Set<ByteBuffer> pointSet = new HashSet<>();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(message);
            byte[] p = byteMulEcc.hashToCurve(message);
            Assert.assertTrue(byteMulEcc.isValidPoint(p));
            pointSet.add(ByteBuffer.wrap(byteMulEcc.hashToCurve(message)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, pointSet.size());
    }

    @Test
    public void testMul() {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        byte[] g = byteMulEcc.getG();
        // 生成一个椭圆曲线点h
        byte[] h = byteMulEcc.randomPoint(SECURE_RANDOM);
        // mulEcc乘法不一定满足可逆性
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 生成r1和r2
            byte[] r1 = byteMulEcc.randomScalar(SECURE_RANDOM);
            byte[] r2 = byteMulEcc.randomScalar(SECURE_RANDOM);
            // g^{r1 * r2}
            byte[] gr1 = byteMulEcc.mul(g, r1);
            byte[] gr12 = byteMulEcc.mul(gr1, r2);
            // g^{r2 * r1}
            byte[] gr2 = byteMulEcc.mul(g, r2);
            byte[] gr21 = byteMulEcc.mul(gr2, r1);
            Assert.assertArrayEquals(gr12, gr21);
            // h^{r1 * r2}
            byte[] hr1 = byteMulEcc.mul(h, r1);
            byte[] hr12 = byteMulEcc.mul(hr1, r2);
            // h^{r2 * r1}
            byte[] hr2 = byteMulEcc.mul(h, r2);
            byte[] hr21 = byteMulEcc.mul(hr2, r1);
            Assert.assertArrayEquals(hr12, hr21);
        }
    }

    @Test
    public void testBaseMul() {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        byte[] g = byteMulEcc.getG();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 生成r
            byte[] r = byteMulEcc.randomScalar(SECURE_RANDOM);
            byte[] gr = byteMulEcc.mul(g, r);
            byte[] baseGr = byteMulEcc.baseMul(r);
            Assert.assertArrayEquals(gr, baseGr);
        }
    }

    @Test
    public void testParallel() {
        ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(byteEccType);
        // HashToCurve并发测试
        byte[][] messages = IntStream.range(0, PARALLEL_NUM)
            .mapToObj(index -> new byte[CommonConstants.BLOCK_BYTE_LENGTH])
            .toArray(byte[][]::new);
        Set<ByteBuffer> hashMessageSet = Arrays.stream(messages)
            .parallel()
            .map(byteMulEcc::hashToCurve)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashMessageSet.size());
        // RandomPoint并发测试
        Set<ByteBuffer> randomPointSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteMulEcc.randomPoint(SECURE_RANDOM))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(PARALLEL_NUM, randomPointSet.size());
        // 乘法并发测试
        byte[] p = byteMulEcc.randomPoint(SECURE_RANDOM);
        byte[] r = byteMulEcc.randomScalar(SECURE_RANDOM);
        Set<ByteBuffer> mulSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteMulEcc.mul(p, r))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, mulSet.size());
    }
}
