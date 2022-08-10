package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.ldp.range.ApacheLaplaceLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpConfig;
import org.junit.Test;

/**
 * 朴素范围整数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public class NaiveRangeIntegralLdpTest extends AbstractIntegralLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig.Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testUnbiasedBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        RangeLdpConfig smallRangeCdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10)
            .build();
        IntegralLdpConfig smallLdpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(smallRangeCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp smallMechanism = IntegralLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        RangeLdpConfig largeRangeCdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig largeLdpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(largeRangeCdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp largeMechanism = IntegralLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        RangeLdpConfig rangeLdpConfig = new ApacheLaplaceLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        IntegralLdpConfig ldpConfig = new NaiveRangeIntegralLdpConfig
            .Builder(rangeLdpConfig, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
