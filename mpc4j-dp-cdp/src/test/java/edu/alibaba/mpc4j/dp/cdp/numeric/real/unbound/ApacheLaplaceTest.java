package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Test;

/**
 * Apache拉普拉斯机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public class ApacheLaplaceTest extends AbstractUnboundRealCdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1.0;

    @Test(expected = AssertionError.class)
    public void testNegativeSensitivity() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, -1.0)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEpsilon() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(-1.0, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroEpsilonZeroDelta() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(0.0, DEFAULT_SENSITIVITY)
            .setDelta(0.0)
            .build();
        UnboundRealCdpFactory.createInstance(cdpConfig);
    }

    @Test
    public void testEpsilonWithZeroDelta() {
        UnboundRealCdpConfig realCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setDelta(0.0)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(realCdpConfig);

        testFunctionality(mechanism);
        testFunctionality(mechanism);
    }

    @Test
    public void testZeroEpsilonWithDelta() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(0.0, DEFAULT_SENSITIVITY)
            .setDelta(0.5)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testDefault() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setDelta(0.01)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 1倍ε
        UnboundRealCdpConfig smallCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp smallMechanism = UnboundRealCdpFactory.createInstance(smallCdpConfig);
        // 10倍ε
        UnboundRealCdpConfig largeCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(10 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp largeMechanism = UnboundRealCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        UnboundRealCdpConfig smallCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp smallMechanism = UnboundRealCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        UnboundRealCdpConfig largeCdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, 10 * DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp largeMechanism = UnboundRealCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        UnboundRealCdpConfig cdpConfig = new ApacheLaplaceCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
