package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.dp.ldp.range.ApacheLaplaceLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpConfig;
import org.junit.Test;

/**
 * 朴素范围LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public class NaiveRangeRealLdpTest extends AbstractRealLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig.Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        RangeLdpConfig smallRangeCdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10)
            .build();
        RealLdpConfig smallLdpConfig = new NaiveRangeRealLdpConfig
            .Builder(smallRangeCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp smallMechanism = RealLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        RangeLdpConfig largeRangeCdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig largeLdpConfig = new NaiveRangeRealLdpConfig
            .Builder(largeRangeCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp largeMechanism = RealLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RealLdpConfig ldpConfig = new NaiveRangeRealLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
