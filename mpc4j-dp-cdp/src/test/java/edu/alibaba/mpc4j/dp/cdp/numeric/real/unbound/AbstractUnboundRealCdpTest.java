package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

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
 * 实数CDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
abstract class AbstractUnboundRealCdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUnboundRealCdpTest.class);
    /**
     * 输出格式
     */
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 测试数量
     */
    private static final int ROUND = 10000;
    /**
     * 默认无偏输入值
     */
    protected static final double UNBIASED_INPUT_VALUE = 0.0;
    /**
     * 默认有偏输入值
     */
    protected static final double BIASED_INPUT_VALUE = 10.0;
    /**
     * 默认Δf
     */
    protected static final double DEFAULT_SENSITIVITY = 1.0;

    protected void testFunctionality(UnboundRealCdp mechanism) {
        LOGGER.info("-----test default-----");
        double noiseValue = mechanism.randomize(UNBIASED_INPUT_VALUE);
        LOGGER.info("{}: input = {}, output = {}", mechanism.getMechanismName(), UNBIASED_INPUT_VALUE, noiseValue);
    }

    protected void testLargeEpsilon(UnboundRealCdp mechanism) {
        LOGGER.info("-----test large ε-----");
        // 敏感度设置为1.0，ε设置为正无穷时，噪声应该无穷接近于0，因此输出结果应该等于有偏真实值
        // 但不能把ε设置得太大，否则e^ε可能过大
        double[] noiseBiasedValue = IntStream.range(0, ROUND)
            .mapToDouble(i -> mechanism.randomize(BIASED_INPUT_VALUE))
            .peek(noiseValue -> Assert.assertEquals(noiseValue, BIASED_INPUT_VALUE, 0.5))
            .toArray();
        double min = Arrays.stream(noiseBiasedValue).min().orElse(0.0);
        double max = Arrays.stream(noiseBiasedValue).max().orElse(0.0);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(),
            BIASED_INPUT_VALUE, DECIMAL_FORMAT.format(min), DECIMAL_FORMAT.format(max)
        );
    }

    protected void testEpsilon(UnboundRealCdp smallMechanism, UnboundRealCdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        double smallEpsilonAbs = IntStream.range(0, ROUND)
            .mapToDouble(index -> smallMechanism.randomize(UNBIASED_INPUT_VALUE))
            .map(Math::abs)
            .sum() / ROUND;
        LOGGER.info("{}, abs = {}", smallMechanism.getMechanismName(), DECIMAL_FORMAT.format(smallEpsilonAbs));
        double largeEpsilonAbs = IntStream.range(0, ROUND)
            .mapToDouble(index -> largeMechanism.randomize(UNBIASED_INPUT_VALUE))
            .map(Math::abs)
            .sum() / ROUND;
        LOGGER.info("{}, abs = {}", largeMechanism.getMechanismName(), DECIMAL_FORMAT.format(largeEpsilonAbs));
        Assert.assertTrue(largeEpsilonAbs < smallEpsilonAbs);
    }

    protected void testSensitivity(UnboundRealCdp smallMechanism, UnboundRealCdp largeMechanism) {
        LOGGER.info("-----test Δf-----");
        double smallEpsilon = smallMechanism.getEpsilon();
        LOGGER.info("{}: Δf = {}", smallMechanism.getMechanismName(), smallMechanism.getSensitivity());
        double largeEpsilon = largeMechanism.getEpsilon();
        LOGGER.info("{}: Δf = {}", largeMechanism.getMechanismName(), largeMechanism.getSensitivity());
        // Δf越大，2 * Δf * ε越大
        Assert.assertTrue(smallEpsilon < largeEpsilon);
    }

    protected void testDistribution(UnboundRealCdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<Integer, Long> histogramMap = IntStream.range(0, ROUND)
            .mapToDouble(index -> mechanism.randomize(UNBIASED_INPUT_VALUE))
            .map(Math::round)
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

    protected void testReseed(UnboundRealCdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            double[] round1s = IntStream.range(0, ROUND)
                .mapToDouble(index -> mechanism.randomize(UNBIASED_INPUT_VALUE))
                .toArray();
            mechanism.reseed(0L);
            double[] round2s = IntStream.range(0, ROUND)
                .mapToDouble(index -> mechanism.randomize(UNBIASED_INPUT_VALUE))
                .toArray();
            Assert.assertArrayEquals(round1s, round2s, DoubleUtils.PRECISION);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
