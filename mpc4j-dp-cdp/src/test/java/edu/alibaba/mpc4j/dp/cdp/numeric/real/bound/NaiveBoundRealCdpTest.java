package edu.alibaba.mpc4j.dp.cdp.numeric.real.bound;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.ApacheLaplaceCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpConfig;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * 朴素有界实数CDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
public class NaiveBoundRealCdpTest extends AbstractBoundRealCdpTest {
    /**
     * 默认ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1.0;

    @Test(expected = AssertionError.class)
    public void testIllegalBounds() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig.Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testEqualBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);
        double noiseValue = mechanism.randomize(UNBIASED_LOWER_BOUND);
        Assert.assertEquals(UNBIASED_LOWER_BOUND, noiseValue, DoubleUtils.PRECISION);
    }

    @Test
    public void testValueEqualLowerBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        UnboundRealCdpConfig smallRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig smallCdpConfig = new NaiveBoundRealCdpConfig
            .Builder(smallRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp smallMechanism = BoundRealCdpFactory.createInstance(smallCdpConfig);
        // 1倍ε
        UnboundRealCdpConfig largeRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig largeCdpConfig = new NaiveBoundRealCdpConfig
            .Builder(largeRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp largeMechanism = BoundRealCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        UnboundRealCdpConfig smallRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig smallCdpConfig = new NaiveBoundRealCdpConfig
            .Builder(smallRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp smallMechanism = BoundRealCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        UnboundRealCdpConfig largeRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY * 10)
            .build();
        BoundRealCdpConfig largeCdpConfig = new NaiveBoundRealCdpConfig
            .Builder(largeRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp largeMechanism = BoundRealCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        UnboundRealCdpConfig unboundRealCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        BoundRealCdpConfig cdpConfig = new NaiveBoundRealCdpConfig
            .Builder(unboundRealCdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundRealCdp mechanism = BoundRealCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
