package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 本地映射实数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/5/3
 */
public class LocalMapRealLdpTest extends AbstractRealLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;
    /**
     * 默认分区长度θ
     */
    private static final double DEFAULT_THETA = 3;
    /**
     * 较小分区长度θ
     */
    private static final double SMALL_THETA = 0.01;

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testSmallThetaZeroInput() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        double value = 0;
        IntStream.range(0, ROUND).forEach(index -> {
            double noiseValue = mechanism.randomize(value);
            Assert.assertTrue(noiseValue < value + SMALL_THETA && noiseValue >= value);
        });
    }

    @Test
    public void testSmallThetaPositiveInput() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        double lowerBound = 1.0;
        SecureRandom secureRandom = new SecureRandom();
        // 输入范围[1.0, 1.0 + 0.01)，输出范围[1.0, 1.0 + 0.01)
        IntStream.range(0, ROUND).forEach(index -> {
            double value = secureRandom.nextDouble() * SMALL_THETA + lowerBound;
            double noiseValue = mechanism.randomize(value);
            Assert.assertTrue(noiseValue < lowerBound + SMALL_THETA && noiseValue >= lowerBound);
        });
    }

    @Test
    public void testSmallThetaNegativeInput() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        double lowerBound = -1.0;
        SecureRandom secureRandom = new SecureRandom();
        // 输入范围[-1.0, -1.0 + 0.01)，输出范围[-1.0, -1.0 + 0.01)
        IntStream.range(0, ROUND).forEach(index -> {
            double value = secureRandom.nextDouble() * SMALL_THETA + lowerBound;
            double noiseValue = mechanism.randomize(value);
            Assert.assertTrue(noiseValue < lowerBound + SMALL_THETA && noiseValue >= lowerBound);
        });
    }

    @Test
    public void testUnbiasedBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        RealLdpConfig smallLdpConfig = new LocalMapRealLdpConfig
            .Builder(0.1 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp smallMechanism = RealLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        RealLdpConfig largeLdpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp largeMechanism = RealLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testSmallThetaDistribution() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        RealLdpConfig ldpConfig = new LocalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
