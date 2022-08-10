package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * 调整映射整数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
@RunWith(Parameterized.class)
public class AdjMapIntegralLdpTest extends AbstractIntegralLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;
    /**
     * 默认分区长度θ
     */
    private static final int DEFAULT_THETA = 3;
    /**
     * 较小分区长度θ
     */
    private static final int SMALL_THETA = 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // α = 0.5
        configurations.add(new Object[]{"α = 0.5", 0.5,});
        // α = 1.0
        configurations.add(new Object[]{"α = 1.0", 1.0,});
        // α = 5.0
        configurations.add(new Object[]{"α = 5.0", 5.0,});

        return configurations;
    }

    private final double alpha;

    public AdjMapIntegralLdpTest(String name, double alpha) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.alpha = alpha;
    }

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        IntegralLdpConfig smallLdpConfig = new AdjMapIntegralLdpConfig
            .Builder(0.1 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp smallMechanism = IntegralLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        IntegralLdpConfig largeLdpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        IntegralLdp largeMechanism = IntegralLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testSmallThetaDistribution() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        IntegralLdpConfig ldpConfig = new AdjMapIntegralLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .setRandom(new Random())
            .build();
        IntegralLdp mechanism = IntegralLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
