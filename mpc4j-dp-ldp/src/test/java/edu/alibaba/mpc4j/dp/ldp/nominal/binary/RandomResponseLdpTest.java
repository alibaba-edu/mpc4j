package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

import org.junit.Test;

import java.util.Random;

/**
 * random response LDP test.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public class RandomResponseLdpTest extends AbstractBinaryLdpTest {
    /**
     * default base ε
     */
    protected static final double DEFAULT_BASE_EPSILON = 1;

    @Test
    public void testDefault() {
        BinaryLdpConfig ldpConfig = new RandomResponseLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .build();
        BinaryLdp mechanism = BinaryLdpFactory.createInstance(ldpConfig);

        testDefault(mechanism);
    }

    @Test
    public void testLargeEpsilon() {
        BinaryLdpConfig ldpConfig = new RandomResponseLdpConfig
            .Builder(100 * DEFAULT_BASE_EPSILON)
            .build();
        BinaryLdp mechanism = BinaryLdpFactory.createInstance(ldpConfig);

        testLargeEpsilon(mechanism);
    }

    @Test
    public void testEpsilon() {
        // 0.1倍ε
        BinaryLdpConfig smallLdpConfig = new RandomResponseLdpConfig
            .Builder(DEFAULT_BASE_EPSILON / 10)
            .build();
        BinaryLdp smallMechanism = BinaryLdpFactory.createInstance(smallLdpConfig);
        // 1倍ε
        BinaryLdpConfig largeLdpConfig = new RandomResponseLdpConfig
            .Builder(10 * DEFAULT_BASE_EPSILON)
            .build();
        BinaryLdp largeMechanism = BinaryLdpFactory.createInstance(largeLdpConfig);

        testEpsilon(smallMechanism, largeMechanism);
    }

    @Test
    public void testReseed() {
        BinaryLdpConfig ldpConfig = new RandomResponseLdpConfig
            .Builder(DEFAULT_BASE_EPSILON)
            .setRandom(new Random())
            .build();
        BinaryLdp mechanism = BinaryLdpFactory.createInstance(ldpConfig);

        testReseed(mechanism);
    }
}
