package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Test;

/**
 * 全局映射实数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/5/3
 */
public class GlobalMapRealLdpTest extends AbstractRealLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        RealLdpConfig smallLdpConfig = new GlobalMapRealLdpConfig
            .Builder(0.1 * DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp smallMechanism = RealLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        RealLdpConfig largeLdpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp largeMechanism = RealLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        RealLdpConfig ldpConfig = new GlobalMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
