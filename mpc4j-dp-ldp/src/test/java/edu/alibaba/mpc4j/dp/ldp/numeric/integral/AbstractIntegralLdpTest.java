package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 整数LDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
abstract class AbstractIntegralLdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegralLdpTest.class);
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

    protected void testFunctionality(IntegralLdp mechanism, int value) {
        LOGGER.info("-----test functionality-----");
        int noiseValue = mechanism.randomize(value);
        LOGGER.info("{}: input = {}, output = {}", mechanism.getMechanismName(), value, noiseValue);
    }

    protected void testDefault(IntegralLdp mechanism) {
        LOGGER.info("-----test default-----");
        // 整数LDP机制输出范围不一定在输入范围内
        int[] noiseValues = IntStream.range(0, ROUND)
            .map(i -> mechanism.randomize(INPUT_VALUE))
            .toArray();
        int min = Arrays.stream(noiseValues).min().orElse(Integer.MIN_VALUE);
        int max = Arrays.stream(noiseValues).max().orElse(Integer.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), INPUT_VALUE, min, max);
    }

    protected void testLargeEpsilon(IntegralLdp mechanism) {
        LOGGER.info("-----test large ε-----");
        // 即使ε比较大，也很难做到输出结果与原始值相同
        int[] noiseValue = IntStream.range(0, ROUND)
            .map(i -> mechanism.randomize(INPUT_VALUE))
            .toArray();
        int min = Arrays.stream(noiseValue).min().orElse(Integer.MIN_VALUE);
        int max = Arrays.stream(noiseValue).max().orElse(Integer.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), INPUT_VALUE, min, max);
    }

    protected void testEpsilon(IntegralLdp smallMechanism, IntegralLdp largeMechanism) {
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

    protected void testDistribution(IntegralLdp mechanism) {
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

    protected void testReseed(IntegralLdp mechanism) {
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
