package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import org.junit.Test;

import java.util.Random;

/**
 * 全局映射指数整数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/7/4
 */
public class GlobalExpMapIntegralLdpTest extends AbstractIntegralLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        IntegralLdpConfig smallLdpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(0.1 * DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp smallMechanism = IntegralLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        IntegralLdpConfig largeLdpConfig = new GlobalMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp largeMechanism = IntegralLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        IntegralLdpConfig ldpConfig = new GlobalExpMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setRandom(new Random())
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
