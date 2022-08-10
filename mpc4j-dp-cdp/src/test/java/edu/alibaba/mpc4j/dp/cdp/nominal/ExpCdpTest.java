package edu.alibaba.mpc4j.dp.cdp.nominal;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 指数CDP机制测试。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/23
 */
public class ExpCdpTest extends AbstractNominalCdpTest {
    /**
     * 默认基础ε
     */
    private static final double DEFAULT_BASE_EPSILON = Math.log(2);

    @Test(expected = AssertionError.class)
    public void testNoUtilityList() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, null)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEpsilon() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(-1.0, defaultNounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testMissingNounPairDistance() {
        List<NounPairDistance> nounPairDistances = new ArrayList<>();
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "B", 1.0));
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "C", 2.0));
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, nounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeNounPairDistance() {
        List<NounPairDistance> nounPairDistances = new ArrayList<>();
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "B", 1.0));
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "C", 2.0));
        nounPairDistances.add(NounPairDistance.createFromNouns("B", "C", -2.0));
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, nounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testOutsideNoun() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);
        mechanism.randomize("D");
    }

    @Test
    public void testDefault() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 1倍ε
        NominalCdpConfig smallCdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .build();
        NominalCdp smallMechanism = NominalCdpFactory.createInstance(smallCdpConfig);
        // 10倍ε
        NominalCdpConfig largeCdpConfig = new ExpCdpConfig
            .Builder(10 * DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .build();
        NominalCdp largeMechanism = NominalCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        NominalCdpConfig cdpConfig = new ExpCdpConfig
            .Builder(DEFAULT_BASE_EPSILON, defaultNounPairDistances)
            .setRandom(new Random())
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);

        testReseed(mechanism);
    }
}
