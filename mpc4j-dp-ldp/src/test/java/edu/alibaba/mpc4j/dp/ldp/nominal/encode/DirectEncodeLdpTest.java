package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 直接编码LDP机制测试。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public class DirectEncodeLdpTest extends AbstractEncodeLdpTest {
    /**
     * 默认基础ε
     */
    protected static final double DEFAULT_BASE_EPSILON = 1;

    @Test(expected = AssertionError.class)
    public void testIllegalLabels() {
        ArrayList<String> illegalLabelList = IntStream.range(0, 1)
            .mapToObj(String::valueOf)
            .collect(Collectors.toCollection(ArrayList::new));
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, illegalLabelList)
            .build();
        EncodeLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalRepeatLabels() {
        ArrayList<String> illegalLabelList = Arrays.stream(new String[]{"0", "0"})
            .collect(Collectors.toCollection(ArrayList::new));
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, illegalLabelList)
            .build();
        EncodeLdpFactory.createInstance(ldpConfig);
    }

    @Test(expected = AssertionError.class)
    public void testIllegalValue() {
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_LABELS)
            .build();
        EncodeLdp mechanism = EncodeLdpFactory.createInstance(ldpConfig);
        mechanism.randomize("-1");
    }

    @Test
    public void testDefault() {
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_LABELS)
            .build();
        EncodeLdp mechanism = EncodeLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON, DEFAULT_LABELS)
            .build();
        EncodeLdp mechanism = EncodeLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        EncodeLdpConfig smallLdpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10, DEFAULT_LABELS)
            .build();
        EncodeLdp smallMechanism = EncodeLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        EncodeLdpConfig largeLdpConfig = new DirectEncodeLdpConfig
            .Builder(10 * DEFAULT_BASE_EPSILON, DEFAULT_LABELS)
            .build();
        EncodeLdp largeMechanism = EncodeLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testDistribution() {
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_LABELS)
            .build();
        EncodeLdp mechanism = EncodeLdpFactory.createInstance(ldpConfig);

        testDistribution(mechanism);
    }

    @Test
    public void testReseed() {
        EncodeLdpConfig ldpConfig = new DirectEncodeLdpConfig
            .Builder(DEFAULT_BASE_EPSILON, DEFAULT_LABELS)
            .setRandom(new Random())
            .build();
        EncodeLdp mechanism = EncodeLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
