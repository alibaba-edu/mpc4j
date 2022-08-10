package edu.alibaba.mpc4j.dp.ldp.range;

import org.junit.Test;

import java.util.Random;

/**
 * Minimax数值型LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
public class MinimaxLdpTest extends AbstractRangeLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1.0;

    @Test(expected = AssertionError.class)
    public void testNegativeEpsilon() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(-1.0)
            .build();
        RangeLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroEpsilon() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(0.0)
            .build();
        RangeLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalSmallValue() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RangeLdp mechanism = RangeLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(NEG_INPUT_VALUE - 1);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalLargeValue() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RangeLdp mechanism = RangeLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(POS_INPUT_VALUE + 1);
    }

    @Test
    public void testDefault() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RangeLdp mechanism = RangeLdpFactory.createInstance(ldpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON)
            .build();
        RangeLdp mechanism = RangeLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.01倍ε
        RangeLdpConfig smallLdpConfig = new MinimaxLdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 100)
            .build();
        RangeLdp smallMechanism = RangeLdpFactory.createInstance(smallLdpConfig);
        // 10倍ε
        RangeLdpConfig largeLdpConfig = new MinimaxLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON)
            .build();
        RangeLdp largeMechanism = RangeLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        RangeLdp mechanism = RangeLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        RangeLdpConfig ldpConfig = new MinimaxLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .setRandom(new Random())
            .build();
        RangeLdp mechanism = RangeLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
