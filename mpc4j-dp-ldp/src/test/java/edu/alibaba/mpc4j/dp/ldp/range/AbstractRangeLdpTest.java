package edu.alibaba.mpc4j.dp.ldp.range;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 数值型LDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
abstract class AbstractRangeLdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRangeLdpTest.class);
    /**
     * 输出格式
     */
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 测试数量
     */
    private static final int ROUND = 1 << 16;
    /**
     * 输入值0
     */
    protected static final double ZERO_INPUT_VALUE = 0.0;
    /**
     * 输入值-1
     */
    protected static final double NEG_INPUT_VALUE = -1.0;
    /**
     * 输入值1
     */
    protected static final double POS_INPUT_VALUE = 1.0;

    protected void testFunctionality(RangeLdp mechanism) {
        LOGGER.info("-----test functionality-----");
        LOGGER.info("{}: {} -> {}, {} -> {}, {} -> {}", mechanism.getMechanismName(),
            NEG_INPUT_VALUE, DECIMAL_FORMAT.format(mechanism.randomize(NEG_INPUT_VALUE)),
            ZERO_INPUT_VALUE, DECIMAL_FORMAT.format(mechanism.randomize(ZERO_INPUT_VALUE)),
            POS_INPUT_VALUE, DECIMAL_FORMAT.format(mechanism.randomize(POS_INPUT_VALUE))
        );
    }

    protected void testLargeEpsilon(RangeLdp mechanism) {
        LOGGER.info("-----test large ε-----");
        LOGGER.info("{}", mechanism.getMechanismName());
        // 输入为0
        testLargeEpsilon(mechanism, ZERO_INPUT_VALUE);
        // 输入为-1
        testLargeEpsilon(mechanism, NEG_INPUT_VALUE);
        // 输入为1
        testLargeEpsilon(mechanism, POS_INPUT_VALUE);
    }

    private void testLargeEpsilon(RangeLdp mechanism, double input) {
        double mean = IntStream.range(0, ROUND)
            .mapToDouble(i -> mechanism.randomize(input))
            .sum() / ROUND;
        LOGGER.info("input = {}, mean = {}", input, DECIMAL_FORMAT.format(mean));
        Assert.assertEquals(input, mean, 0.1);
    }

    protected void testEpsilon(RangeLdp smallMechanism, RangeLdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        // 输入为0
        testEpsilon(smallMechanism, largeMechanism, 0.0);
        // 输入为-1
        testEpsilon(smallMechanism, largeMechanism, -1.0);
        // 输入为1
        testEpsilon(smallMechanism, largeMechanism, 1.0);
    }

    private void testEpsilon(RangeLdp smallMechanism, RangeLdp largeMechanism, double input) {
        double smallEpsilonMean = IntStream.range(0, ROUND)
            .mapToDouble(index -> smallMechanism.randomize(input))
            .sum() / ROUND;
        double smallEpsilonMeanError = Math.abs(input - smallEpsilonMean);
        LOGGER.info("{}: input = {}, mean = {}",
            smallMechanism.getMechanismName(), input, DECIMAL_FORMAT.format(smallEpsilonMean)
        );
        double largeEpsilonMean = IntStream.range(0, ROUND)
            .mapToDouble(index -> largeMechanism.randomize(input))
            .sum() / ROUND;
        double largeEpsilonMeanError = Math.abs(input - largeEpsilonMean);
        LOGGER.info("{}: input = {}, mean = {}",
            largeMechanism.getMechanismName(), input, DECIMAL_FORMAT.format(largeEpsilonMean)
        );
        Assert.assertTrue(largeEpsilonMeanError < smallEpsilonMeanError);
    }

    protected void testDistribution(RangeLdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<Integer, Long> histogramMap = IntStream.range(0, ROUND)
            .mapToDouble(index -> mechanism.randomize(ZERO_INPUT_VALUE))
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

    protected void testReseed(RangeLdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            double[] round1s = IntStream.range(0, ROUND)
                .mapToDouble(index -> mechanism.randomize(ZERO_INPUT_VALUE))
                .toArray();
            mechanism.reseed(0L);
            double[] round2s = IntStream.range(0, ROUND)
                .mapToDouble(index -> mechanism.randomize(ZERO_INPUT_VALUE))
                .toArray();
            Assert.assertArrayEquals(round1s, round2s, DoubleUtils.PRECISION);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
