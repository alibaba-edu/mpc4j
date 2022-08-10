package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * 本地映射整数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
public class LocalMapIntegralLdpTest extends AbstractIntegralLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;
    /**
     * 默认分区长度θ
     */
    private static final int DEFAULT_THETA = 3;
    /**
     * 较小分区长度θ
     */
    private static final int SMALL_THETA = 1;

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testSmallThetaZeroInput() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        int value = 0;
        IntStream.range(0, ROUND).forEach(index -> {
            int noiseValue = mechanism.randomize(value);
            Assert.assertEquals(value, noiseValue);
        });
    }

    @Test
    public void testSmallThetaPositiveInput() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        int value = 1;
        // 输入1，输出[1, 2)
        IntStream.range(0, ROUND).forEach(index -> {
            int noiseValue = mechanism.randomize(value);
            Assert.assertEquals(value, noiseValue);
        });
    }

    @Test
    public void testSmallThetaNegativeInput() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        int value = -1;
        // 输入-1，输出[-1, 0)
        IntStream.range(0, ROUND).forEach(index -> {
            int noiseValue = mechanism.randomize(value);
            Assert.assertEquals(value, noiseValue);
        });
    }

    @Test
    public void testUnbiasedBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        IntegralLdpConfig smallLdpConfig = new LocalMapIntegralLdpConfig
            .Builder(0.1 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp smallMechanism = IntegralLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        IntegralLdpConfig largeLdpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp largeMechanism = IntegralLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testSmallThetaDistribution() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        IntegralLdpConfig ldpConfig = new LocalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setRandom(new Random())
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
