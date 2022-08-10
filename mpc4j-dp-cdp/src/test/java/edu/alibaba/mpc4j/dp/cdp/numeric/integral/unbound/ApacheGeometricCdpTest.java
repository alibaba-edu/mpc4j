package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric.ApacheGeometricCdpConfig;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Test;

/**
 * Apache几何CDP机制测试。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/23
 */
public class ApacheGeometricCdpTest extends AbstractUnboundIntegralCdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1.0;

    @Test(expected = AssertionError.class)
    public void testNegativeSensitivity() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, -1)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEpsilon() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(-1.0, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroEpsilon() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(0.0, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test
    public void testDefault() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        UnboundIntegralCdpConfig smallCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp smallMechanism = UnboundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 1倍ε
        UnboundIntegralCdpConfig largeCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp largeMechanism = UnboundIntegralCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        UnboundIntegralCdpConfig smallCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp smallMechanism = UnboundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        UnboundIntegralCdpConfig largeCdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, 10 * DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp largeMechanism = UnboundIntegralCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        UnboundIntegralCdpConfig cdpConfig = new ApacheGeometricCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_SENSITIVITY)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
