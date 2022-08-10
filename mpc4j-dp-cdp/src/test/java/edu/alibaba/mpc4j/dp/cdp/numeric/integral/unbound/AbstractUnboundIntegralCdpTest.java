package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 整数CDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
abstract class AbstractUnboundIntegralCdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUnboundIntegralCdpTest.class);
    /**
     * 测试轮数
     */
    protected static final int ROUND = 10000;
    /**
     * 默认输入值为0
     */
    protected static final int UNBIASED_INPUT_VALUE = 0;
    /**
     * 默认有偏输入值为1
     */
    protected static final int BIASED_INPUT_VALUE = 10;
    /**
     * 默认Δf
     */
    protected static final int DEFAULT_SENSITIVITY = 1;

    protected void testFunctionality(UnboundIntegralCdp mechanism) {
        LOGGER.info("-----test functionality-----");
        int noiseValue = mechanism.randomize(UNBIASED_INPUT_VALUE);
        LOGGER.info("{}: input = {}, output = {}", mechanism.getMechanismName(), UNBIASED_INPUT_VALUE, noiseValue);
    }

    protected void testLargeEpsilon(UnboundIntegralCdp mechanism) {
        LOGGER.info("-----test large ε-----");
        // 敏感度设置为1.0，ε设置为正无穷时，噪声应该无穷接近于0，因此输出结果应该等于有偏真实值，但不能把ε设置得太大，否则e^ε可能过大
        int[] noiseBiasedValue = IntStream.range(0, ROUND)
            .map(i -> mechanism.randomize(BIASED_INPUT_VALUE))
            .peek(noiseValue -> Assert.assertEquals(noiseValue, BIASED_INPUT_VALUE))
            .toArray();
        int min = Arrays.stream(noiseBiasedValue).min().orElse(Integer.MIN_VALUE);
        int max = Arrays.stream(noiseBiasedValue).max().orElse(Integer.MAX_VALUE);
        LOGGER.info("{}: input = {}, min = {}, max = {}", mechanism.getMechanismName(), BIASED_INPUT_VALUE, min, max);
    }

    protected void testEpsilon(UnboundIntegralCdp smallMechanism, UnboundIntegralCdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        int smallEpsilonAbs = IntStream.range(0, ROUND)
            .map(index -> smallMechanism.randomize(UNBIASED_INPUT_VALUE))
            .map(Math::abs)
            .sum();
        LOGGER.info("{}: abs = {}", smallMechanism.getMechanismName(), smallEpsilonAbs);
        int largeEpsilonAbs = IntStream.range(0, ROUND)
            .map(index -> largeMechanism.randomize(UNBIASED_INPUT_VALUE))
            .map(Math::abs)
            .sum();
        LOGGER.info("{}: abs = {}", largeMechanism.getMechanismName(), largeEpsilonAbs);
        Assert.assertTrue(largeEpsilonAbs < smallEpsilonAbs);
    }

    protected void testSensitivity(UnboundIntegralCdp smallMechanism, UnboundIntegralCdp largeMechanism) {
        LOGGER.info("-----test Δf-----");
        double smallEpsilon = smallMechanism.getEpsilon();
        LOGGER.info("{}: Δf = {}", smallMechanism.getMechanismName(), smallMechanism.getSensitivity());
        double largeEpsilon = largeMechanism.getEpsilon();
        LOGGER.info("{}: Δf = {}", largeMechanism.getMechanismName(), largeMechanism.getSensitivity());
        // Δf越大，f * ε越大
        Assert.assertTrue(smallEpsilon < largeEpsilon);
    }

    protected void testDistribution(UnboundIntegralCdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<Integer, Long> histogramMap = IntStream.range(0, ROUND)
            .map(index -> mechanism.randomize(UNBIASED_INPUT_VALUE))
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

    protected void testReseed(UnboundIntegralCdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            int[] round1s = IntStream.range(0, ROUND)
                .map(index -> mechanism.randomize(UNBIASED_INPUT_VALUE))
                .toArray();
            mechanism.reseed(0L);
            int[] round2s = IntStream.range(0, ROUND)
                .map(index -> mechanism.randomize(UNBIASED_INPUT_VALUE))
                .toArray();
            Assert.assertArrayEquals(round1s, round2s);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
