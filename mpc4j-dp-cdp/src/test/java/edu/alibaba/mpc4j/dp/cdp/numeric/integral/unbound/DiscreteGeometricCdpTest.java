package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric.DiscreteGeometricCdpConfig;
import org.junit.Test;

import java.util.Random;

/**
 * 离散几何CDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
public class DiscreteGeometricCdpTest extends AbstractUnboundIntegralCdpTest {
    /**
     * 默认t
     */
    private static final int DEFAULT_T = 1;
    /**
     * 默认s
     */
    private static final int DEFAULT_S = 1;

    @Test(expected = AssertionError.class)
    public void testNegativeSensitivity() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, -1)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeT() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(-1, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroT() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(0, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeS() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, -1, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testZeroS() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, 0, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdpFactory.createInstance(cdpConfig);
    }

    @Test
    public void testDefault() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        // Δf / ε = t / s，因此ε = Δf * s / t，即放大ε相当于放大s
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, 100 * DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // Δf / ε = t / s，因此ε = Δf * s / t，即缩小ε相当于放大t
        UnboundIntegralCdpConfig smallCdpConfig = new DiscreteGeometricCdpConfig
            .Builder(10 * DEFAULT_T, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp smallMechanism = UnboundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 1倍ε
        UnboundIntegralCdpConfig largeCdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp largeMechanism = UnboundIntegralCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testSensitivity() {
        // 1倍Δf
        UnboundIntegralCdpConfig smallCdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp smallMechanism = UnboundIntegralCdpFactory.createInstance(smallCdpConfig);
        // 10倍Δf
        UnboundIntegralCdpConfig largeCdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, 10 * DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp largeMechanism = UnboundIntegralCdpFactory.createInstance(largeCdpConfig);

        testSensitivity(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, DEFAULT_SENSITIVITY)
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        UnboundIntegralCdpConfig cdpConfig = new DiscreteGeometricCdpConfig
            .Builder(DEFAULT_T, DEFAULT_S, DEFAULT_SENSITIVITY)
            .setRandom(new Random())
            .build();
        UnboundIntegralCdp mechanism = UnboundIntegralCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
