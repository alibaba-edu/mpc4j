package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric.ApacheGeometricCdpConfig;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * 朴素有界CDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public class NaiveBoundIntegralCdpTest extends AbstractBoundIntegralCdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = Math.log(2);

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testEqualBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        int noiseValue = mechanism.randomize(UNBIASED_LOWER_BOUND);
        Assert.assertEquals(UNBIASED_LOWER_BOUND, noiseValue);
    }

    @Test
    public void testValueEqualLowerBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        UnboundIntegralCdpConfig smallIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig smallCdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(smallIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp smallMechanism = BoundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 1倍ε
        UnboundIntegralCdpConfig largeIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig largeCdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(largeIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp largeMechanism = BoundIntegralCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        UnboundIntegralCdpConfig smallIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig smallCdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(smallIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp smallMechanism = BoundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        UnboundIntegralCdpConfig largeIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, 10 * DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig largeCdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(largeIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp largeMechanism = BoundIntegralCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        UnboundIntegralCdpConfig unboundIntegralCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        BoundIntegralCdpConfig cdpConfig = new NaiveBoundIntegralCdpConfig
            .Builder(unboundIntegralCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
