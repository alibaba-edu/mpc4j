package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 调整映射实数LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/5/3
 */
@RunWith(Parameterized.class)
public class AdjMapRealLdpTest extends AbstractRealLdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = 1;
    /**
     * 默认分区长度θ
     */
    private static final double DEFAULT_THETA = 3;
    /**
     * 较小分区长度θ
     */
    private static final double SMALL_THETA = 0.01;

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

    public AdjMapRealLdpTest(String name, double alpha) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.alpha = alpha;
    }

    @Test(expected = AssertionError.class)
    public void testIllegalBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_UPPER_BOUND, UNBIASED_LOWER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testEqualBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_LOWER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testValueLessLowerBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_LOWER_BOUND - 1);
    }

    @Test(expected = AssertionError.class)
    public void testValueGreatUpperBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        mechanism.randomize(UNBIASED_UPPER_BOUND + 1);
    }

    @Test
    public void testValueEqualLowerBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_LOWER_BOUND);
    }

    @Test
    public void testValueEqualUpperBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);
        testFunctionality(mechanism, UNBIASED_UPPER_BOUND);
    }

    @Test
    public void testUnbiasedBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testBiasedBound() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        RealLdpConfig smallLdpConfig = new AdjMapRealLdpConfig
            .Builder(0.1 * DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp smallMechanism = RealLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        RealLdpConfig largeLdpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .build();
        RealLdp largeMechanism = RealLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testUnbiasedDistribution() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testBiasedDistribution() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, BIASED_LOWER_BOUND, BIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testSmallThetaDistribution() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, SMALL_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        RealLdpConfig ldpConfig = new AdjMapRealLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_THETA, UNBIASED_LOWER_BOUND, UNBIASED_UPPER_BOUND)
            .setAlpha(alpha)
            .setRandomGenerator(new JDKRandomGenerator())
            .build();
        RealLdp mechanism = RealLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
