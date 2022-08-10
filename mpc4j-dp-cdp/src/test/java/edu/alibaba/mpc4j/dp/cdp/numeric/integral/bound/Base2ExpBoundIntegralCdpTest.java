package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Base2指数有界整数CDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public class Base2ExpBoundIntegralCdpTest extends AbstractBoundIntegralCdpTest {
    /**
     * 参数η_x的默认值
     */
    private static final int ETA_X = 1;
    /**
     * 参数η_y的默认值
     */
    private static final int ETA_Y = 1;

    @Test(expected = AssertionError.class)
    public void testNegativeEtaX() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(-1, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEtaY() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, -1, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEtaZ() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setEtaZ(-1)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalEta() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(4, 1, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalPrecision() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setPrecision(0)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testUnsatisfiedPrecision() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setPrecision(2)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testEqualBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        int noiseValue = mechanism.randomize(UNBIASED_LOWER_BOUND);
        Assert.assertEquals(UNBIASED_LOWER_BOUND, noiseValue);
    }

    @Test
    public void testValueEqualLowerBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);
        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, CommonConstants.STATS_BIT_LENGTH, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 1倍ε
        BoundIntegralCdpConfig smallCdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp smallMechanism = BoundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 10倍ε
        BoundIntegralCdpConfig largeCdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, 10 * ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp largeMechanism = BoundIntegralCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        BoundIntegralCdpConfig smallCdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp smallMechanism = BoundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        BoundIntegralCdpConfig largeCdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, 10 * DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp largeMechanism = BoundIntegralCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        BoundIntegralCdpConfig cdpConfig = new Base2ExpBoundIntegralCdpConfig
            .Builder(ETA_X, ETA_Y, DEFAULT_SENSITIVITY, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setRandom(new Random())
            .build();
        BoundIntegralCdp mechanism = BoundIntegralCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
