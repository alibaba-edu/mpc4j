package edu.alibaba.mpc4j.dp.cdp.nominal;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Base2指数机制测试。
 *
 * @author Xiaodong Zhang, Weiran Liu
 * @date 2022/4/23
 */
public class Base2ExpCdpTest extends AbstractNominalCdpTest {
    /**
     * 默认参数η_x
     */
    private static final int DEFAULT_ETA_X = 1;
    /**
     * 默认参数η_y
     */
    private static final int DEFAULT_ETA_Y = 1;

    @Test(expected = AssertionError.class)
    public void testNoUtilityList() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, null)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEtaX() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(-1, DEFAULT_ETA_Y, defaultNounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEtaY() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, -1, defaultNounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeEtaZ() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .setEtaZ(-1)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalEta() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(4, 1, defaultNounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalPrecision() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .setPrecision(0)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testUnsatisfiedPrecision() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .setPrecision(2)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testMissingNounPairDistance() {
        List<NounPairDistance> nounPairDistances = new ArrayList<>();
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "B", 1.0));
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "C", 2.0));
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, nounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testNegativeNounPairDistance() {
        List<NounPairDistance> nounPairDistances = new ArrayList<>();
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "B", 1.0));
        nounPairDistances.add(NounPairDistance.createFromNouns("A", "C", 2.0));
        nounPairDistances.add(NounPairDistance.createFromNouns("B", "C", -2.0));
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, nounPairDistances)
            .build();
        NominalCdpFactory.createInstance(cdpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testOutsideNoun() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);
        mechanism.randomize("D");
    }

    @Test
    public void testDefault() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, CommonConstants.STATS_BIT_LENGTH, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);

        testFunctionality(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, CommonConstants.STATS_BIT_LENGTH, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 1倍ε
        NominalCdpConfig smallCdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .build();
        NominalCdp smallMechanism = NominalCdpFactory.createInstance(smallCdpConfig);
        // 10倍ε
        NominalCdpConfig largeCdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, 10 * DEFAULT_ETA_Y, defaultNounPairDistances)
            .build();
        NominalCdp largeMechanism = NominalCdpFactory.createInstance(largeCdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        // 此参数设置使得算法接近于base = e时 4ln(2)-DP保护程度，其中Δq = 2
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);
        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        NominalCdpConfig cdpConfig = new Base2ExpCdpConfig
            .Builder(DEFAULT_ETA_X, DEFAULT_ETA_Y, defaultNounPairDistances)
            .setRandom(new Random())
            .setUtilityRandom(new Random())
            .build();
        NominalCdp mechanism = NominalCdpFactory.createInstance(cdpConfig);
        mechanism.setup(cdpConfig);

        testReseed(mechanism);
    }
}
