package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import org.junit.Test;

import java.util.Random;

/**
 * 阶梯机制测试。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/21
 */
public class StaircaseCdpTest extends AbstractUnboundRealCdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1.0;

    @Test(expected = AssertionError.class)
    public void testNegativeSensitivity() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, -1.0)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEpsilon() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(-1.0, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroEpsilon() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(0.0, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeGamma() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setGamma(-1.0)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalGamma() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setGamma(10.0)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test
    public void testDefault() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testZeroGamma() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setGamma(0.0)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testOneGamma() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setGamma(1.0)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 1倍ε
        UnboundRealCdpConfig smallCdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp smallMechanism = UnboundRealCdpFactory.createInstance(smallCdpConfig);
        // 10倍ε
        UnboundRealCdpConfig largeCdpConfig = new StaircaseCdpConfig
            .Builder(10 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp largeMechanism = UnboundRealCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        UnboundRealCdpConfig smallCdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp smallMechanism = UnboundRealCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        UnboundRealCdpConfig largeCdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, 10 * DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp largeMechanism = UnboundRealCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        UnboundRealCdpConfig cdpConfig = new StaircaseCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setRandom(new Random())
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
