package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * 指数有界CDP机制测试。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/24
 */
public class ExpBoundIntegralCdpTest extends AbstractBoundIntegralCdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = Math.log(2);

    @Test(expected = AssertionError.class)
    public void testNegativeEpsilon() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(-1.0, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroEpsilon() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(0.0, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testEqualBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        int noiseValue = mechanism.randomize(UNBIASED_LOWER_BOUND);
        Assert.assertEquals(UNBIASED_LOWER_BOUND, noiseValue);
    }

    @Test
    public void testValueEqualLowerBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        BoundIntegralCdpConfig smallCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp smallMechanism = BoundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 1倍ε
        BoundIntegralCdpConfig largeCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp largeMechanism = BoundIntegralCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        BoundIntegralCdpConfig smallCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp smallMechanism = BoundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        BoundIntegralCdpConfig largeCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, 10 * DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp largeMechanism = BoundIntegralCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        BoundIntegralCdpConfig cdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setRandom(new Random())
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
