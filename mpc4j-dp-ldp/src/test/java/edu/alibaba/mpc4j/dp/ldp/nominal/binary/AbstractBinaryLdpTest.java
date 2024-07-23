package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * abstract binary LDP test.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
abstract class AbstractBinaryLdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBinaryLdpTest.class);
    /**
     * test round
     */
    protected static final int ROUND = 10000;

    protected void testDefault(BinaryLdp mechanism) {
        LOGGER.info("-----test default-----");
        int falseNoiseCount = 0;
        int trueNoiseCount = 0;
        for (int i = 0; i < ROUND; i++) {
            if (!mechanism.randomize(false)) {
                falseNoiseCount++;
            }
            if (mechanism.randomize(true)) {
                trueNoiseCount++;
            }
        }
        double falseRate = (double) falseNoiseCount / ROUND;
        double trueRate = (double) trueNoiseCount / ROUND;
        LOGGER.info("{}: false rate = {}, true rate = {}", mechanism.getMechanismName(), falseRate, trueRate);
        Assert.assertTrue(falseRate > 0.5);
        Assert.assertTrue(trueRate > 0.5);
    }

    protected void testLargeEpsilon(BinaryLdp mechanism) {
        LOGGER.info("-----test large ε-----");
        int falseNoiseCount = 0;
        int trueNoiseCount = 0;
        for (int i = 0; i < ROUND; i++) {
            if (!mechanism.randomize(false)) {
                falseNoiseCount++;
            }
            if (mechanism.randomize(true)) {
                trueNoiseCount++;
            }
        }
        double falseRate = (double) falseNoiseCount / ROUND;
        double trueRate = (double) trueNoiseCount / ROUND;
        LOGGER.info("{}: false rate = {}, true rate = {}", mechanism.getMechanismName(), falseRate, trueRate);
        Assert.assertTrue(falseRate > 0.9);
        Assert.assertTrue(trueRate > 0.9);
    }

    protected void testEpsilon(BinaryLdp smallMechanism, BinaryLdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        long smallEpsilonSameCount = IntStream.range(0, ROUND)
            .filter(index -> !smallMechanism.randomize(false))
            .count();
        LOGGER.info("{}: same num. = {}", smallMechanism.getMechanismName(), smallEpsilonSameCount);
        long largeEpsilonSameCount = IntStream.range(0, ROUND)
            .filter(index -> !largeMechanism.randomize(false))
            .count();
        LOGGER.info("{}: same num. = {}", largeMechanism.getMechanismName(), largeEpsilonSameCount);
        Assert.assertTrue(smallEpsilonSameCount < largeEpsilonSameCount);
    }

    protected void testReseed(BinaryLdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            boolean[] round1s = new boolean[ROUND];
            for (int i = 0; i < ROUND; i++) {
                round1s[i] = mechanism.randomize(false);
            }
            mechanism.reseed(0L);
            boolean[] round2s = new boolean[ROUND];
            for (int i = 0; i < ROUND; i++) {
                round2s[i] = mechanism.randomize(false);
            }
            Assert.assertArrayEquals(round1s, round2s);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
