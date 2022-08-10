package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 有界整数CDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
class AbstractBoundIntegralCdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBoundIntegralCdpTest.class);
    /**
     * 测试轮数
     */
    protected static final int ROUND = 10000;
    /**
     * 默认上下界距离
     */
    protected static final int DEFAULT_BOUND = 10;
    /**
     * 无偏下界
     */
    protected static final int UNBIASED_LOWER_BOUND = 0;
    /**
     * 无偏上界为
     */
    protected static final int UNBIASED_UPPER_BOUND = UNBIASED_LOWER_BOUND + DEFAULT_BOUND;
    /**
     * 有偏下界
     */
    protected static final int BIASED_LOWER_BOUND = -3;
    /**
     * 有偏上界
     */
    protected static final int BIASED_UPPER_BOUND = BIASED_LOWER_BOUND + DEFAULT_BOUND;
    /**
     * 默认输入，同时在无偏/有偏上下界中
     */
    protected static final int INPUT_VALUE = 0;
    /**
     * 默认Δf
     */
    protected static final int DEFAULT_SENSITIVITY = 1;

    protected void testFunctionality(BoundIntegralCdp mechanism, int value) {
        LOGGER.info("-----test functionality-----");
        int noiseValue = mechanism.randomize(value);
        LOGGER.info("{}: input = {}, output = {}", mechanism.getMechanismName(), value, noiseValue);
    }

    protected void testDefault(BoundIntegralCdp mechanism) {
        LOGGER.info("-----test default-----");
        int lowerBound = mechanism.getLowerBound();
        int upperBound = mechanism.getUpperBound();
        int[] noiseValues = IntStream.range(0, ROUND)
            .map(i -> mechanism.randomize(INPUT_VALUE))
            .peek(noiseValue -> Assert.assertTrue(noiseValue >= lowerBound))
            .peek(noiseValue -> Assert.assertTrue(noiseValue <= upperBound))
            .toArray();
        int min = Arrays.stream(noiseValues).min().orElse(Integer.MIN_VALUE);
        int max = Arrays.stream(noiseValues).max().orElse(Integer.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), INPUT_VALUE, min, max);
    }

    protected void testLargeEpsilon(BoundIntegralCdp mechanism) {
        LOGGER.info("-----test large ε-----");
        int[] noiseValues = IntStream.range(0, ROUND)
            .map(i -> mechanism.randomize(INPUT_VALUE))
            .peek(noiseValue -> Assert.assertEquals(INPUT_VALUE, noiseValue))
            .toArray();
        int min = Arrays.stream(noiseValues).min().orElse(Integer.MIN_VALUE);
        int max = Arrays.stream(noiseValues).max().orElse(Integer.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), INPUT_VALUE, min, max);
    }

    protected void testEpsilon(BoundIntegralCdp smallMechanism, BoundIntegralCdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        long smallEpsilonSameCount = IntStream.range(0, ROUND)
            .map(index -> smallMechanism.randomize(INPUT_VALUE))
            .filter(noiseValue -> noiseValue == INPUT_VALUE)
            .count();
        LOGGER.info("{}: same num. = {}", smallMechanism.getMechanismName(), smallEpsilonSameCount);
        long largeEpsilonSameCount = IntStream.range(0, ROUND)
            .map(index -> largeMechanism.randomize(INPUT_VALUE))
            .filter(noiseValue -> noiseValue == INPUT_VALUE)
            .count();
        LOGGER.info("{}: same num. = {}", largeMechanism.getMechanismName(), largeEpsilonSameCount);

        Assert.assertTrue(smallEpsilonSameCount < largeEpsilonSameCount);
    }

    protected void testSensitivity(BoundIntegralCdp smallMechanism, BoundIntegralCdp largeMechanism) {
        LOGGER.info("-----test Δf-----");
        double smallEpsilon = smallMechanism.getEpsilon();
        LOGGER.info("{}: Δf = {}", smallMechanism.getMechanismName(), smallMechanism.getSensitivity());
        double largeEpsilon = largeMechanism.getEpsilon();
        LOGGER.info("{}: Δf = {}", largeMechanism.getMechanismName(), largeMechanism.getSensitivity());
        // Δf越大，2 * Δf * ε越大
        Assert.assertTrue(smallEpsilon < largeEpsilon);
    }

    protected void testDistribution(BoundIntegralCdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<Integer, Long> histogramMap = IntStream.range(0, ROUND)
            .map(index -> mechanism.randomize(INPUT_VALUE))
            .boxed()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String histogram = Arrays.toString(
            histogramMap.keySet().stream()
                .sorted()
                .map(noiseValue -> noiseValue + ": " + histogramMap.get(noiseValue))
                .toArray(String[]::new)
        );
        LOGGER.info("{}: {}", mechanism.getMechanismName(), histogram);
    }

    protected void testReseed(BoundIntegralCdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            int[] round1s = IntStream.range(0, ROUND)
                .map(index -> mechanism.randomize(INPUT_VALUE))
                .toArray();
            mechanism.reseed(0L);
            int[] round2s = IntStream.range(0, ROUND)
                .map(index -> mechanism.randomize(INPUT_VALUE))
                .toArray();
            Assert.assertArrayEquals(round1s, round2s);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
