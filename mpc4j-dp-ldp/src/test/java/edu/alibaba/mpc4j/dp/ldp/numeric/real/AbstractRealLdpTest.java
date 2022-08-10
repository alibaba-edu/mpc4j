package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 实数LDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
abstract class AbstractRealLdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRealLdpTest.class);
    /**
     * 测试轮数
     */
    protected static final int ROUND = 10000;
    /**
     * 默认上下界距离
     */
    protected static final double DEFAULT_BOUND = 10.0;
    /**
     * 无偏下界
     */
    protected static final double UNBIASED_LOWER_BOUND = 0.0;
    /**
     * 无偏上界
     */
    protected static final double UNBIASED_UPPER_BOUND = UNBIASED_LOWER_BOUND + DEFAULT_BOUND;
    /**
     * 有偏下界
     */
    protected static final double BIASED_LOWER_BOUND = -3;
    /**
     * 有偏上界
     */
    protected static final double BIASED_UPPER_BOUND = BIASED_LOWER_BOUND + DEFAULT_BOUND;
    /**
     * 默认输入，同时在无偏/有偏上下界中
     */
    protected static final double INPUT_VALUE = 0.0;

    protected void testFunctionality(RealLdp mechanism, double value) {
        LOGGER.info("-----test functionality-----");
        double noiseValue = mechanism.randomize(value);
        LOGGER.info("{}: input = {}, output = {}", mechanism.getMechanismName(), value, noiseValue);
    }

    protected void testDefault(RealLdp mechanism) {
        LOGGER.info("-----test default-----");
        // 实数LDP机制输出范围不一定在输入范围内
        double[] noiseValues = IntStream.range(0, ROUND)
            .mapToDouble(i -> mechanism.randomize(INPUT_VALUE))
            .toArray();
        double min = Arrays.stream(noiseValues).min().orElse(Double.MIN_VALUE);
        double max = Arrays.stream(noiseValues).max().orElse(Double.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), INPUT_VALUE, min, max);
    }

    protected void testLargeEpsilon(RealLdp mechanism) {
        LOGGER.info("-----test large ε-----");
        // 即使ε比较大，也很难做到输出结果与原始值相同
        double[] noiseValue = IntStream.range(0, ROUND)
            .mapToDouble(i -> mechanism.randomize(INPUT_VALUE))
            .toArray();
        double min = Arrays.stream(noiseValue).min().orElse(Double.MIN_VALUE);
        double max = Arrays.stream(noiseValue).max().orElse(Double.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), INPUT_VALUE, min, max);
    }

    protected void testEpsilon(RealLdp smallMechanism, RealLdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        double smallEpsilonAbs = IntStream.range(0, ROUND)
            .mapToDouble(index -> smallMechanism.randomize(INPUT_VALUE))
            .map(noiseValue -> noiseValue - INPUT_VALUE)
            .map(Math::abs)
            .sum();
        LOGGER.info("{}: abs. = {}", smallMechanism.getMechanismName(), smallEpsilonAbs);
        double largeEpsilonAbs = IntStream.range(0, ROUND)
            .mapToDouble(index -> largeMechanism.randomize(INPUT_VALUE))
            .map(noiseValue -> noiseValue - INPUT_VALUE)
            .map(Math::abs)
            .sum();
        LOGGER.info("{}: abs. = {}", largeMechanism.getMechanismName(), largeEpsilonAbs);

        Assert.assertTrue(largeEpsilonAbs < smallEpsilonAbs);
    }

    protected void testDistribution(RealLdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<Integer, Long> histogramMap = IntStream.range(0, ROUND)
            .mapToDouble(index -> mechanism.randomize(INPUT_VALUE))
            .mapToInt(roundValue -> {
                if (roundValue < 0) {
                    return (int)roundValue - 1;
                } else {
                    return (int)roundValue;
                }
            })
            .boxed()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String histogram = Arrays.toString(
            histogramMap.keySet().stream()
                .sorted()
                .map(noiseValue -> "[" + noiseValue + ", " + (noiseValue + 1) + "): " + histogramMap.get(noiseValue))
                .toArray(String[]::new)
        );
        LOGGER.info("{}: {}", mechanism.getMechanismName(), histogram);
    }

    protected void testReseed(RealLdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            double[] round1s = IntStream.range(0, ROUND)
                .mapToDouble(index -> mechanism.randomize(INPUT_VALUE))
                .toArray();
            mechanism.reseed(0L);
            double[] round2s = IntStream.range(0, ROUND)
                .mapToDouble(index -> mechanism.randomize(INPUT_VALUE))
                .toArray();
            Assert.assertArrayEquals(round1s, round2s, DoubleUtils.PRECISION);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }

}
