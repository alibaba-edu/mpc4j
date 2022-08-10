package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 编码LDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
abstract class AbstractEncodeLdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEncodeLdpTest.class);
    /**
     * 测试轮数
     */
    protected static final int ROUND = 10000;
    /**
     * 默认标签取值列表
     */
    protected static final ArrayList<String> DEFAULT_LABELS = IntStream.range(0, 10)
        .mapToObj(String::valueOf)
        .collect(Collectors.toCollection(ArrayList::new));
    /**
     * 默认输入
     */
    protected static final String INPUT_VALUE = "0";

    protected void testDefault(EncodeLdp mechanism) {
        LOGGER.info("-----test default-----");
        Set<String> noiseValueSet = IntStream.range(0, ROUND)
            .mapToObj(i -> mechanism.randomize(INPUT_VALUE))
            .collect(Collectors.toSet());
        LOGGER.info("{}: input = {}, output range = {}", mechanism.getMechanismName(), INPUT_VALUE, noiseValueSet);
    }

    protected void testLargeEpsilon(EncodeLdp mechanism) {
        LOGGER.info("-----test large ε-----");
        // 即使ε比较大，也很难做到输出结果与原始值相同
        Set<String> noiseValueSet = IntStream.range(0, ROUND)
            .mapToObj(i -> mechanism.randomize(INPUT_VALUE))
            .collect(Collectors.toSet());
        LOGGER.info("{}: input = {}, output range = {}", mechanism.getMechanismName(), INPUT_VALUE, noiseValueSet);
    }

    protected void testEpsilon(EncodeLdp smallMechanism, EncodeLdp largeMechanism) {
        LOGGER.info("-----test ε-----");
        long smallEpsilonSameCount = IntStream.range(0, ROUND)
            .mapToObj(index -> smallMechanism.randomize(INPUT_VALUE))
            .filter(noiseValue -> noiseValue.equals(INPUT_VALUE))
            .count();
        LOGGER.info("{}: same num. = {}", smallMechanism.getMechanismName(), smallEpsilonSameCount);
        long largeEpsilonSameCount = IntStream.range(0, ROUND)
            .mapToObj(index -> largeMechanism.randomize(INPUT_VALUE))
            .filter(noiseValue -> noiseValue.equals(INPUT_VALUE))
            .count();
        LOGGER.info("{}: same num. = {}", largeMechanism.getMechanismName(), largeEpsilonSameCount);

        Assert.assertTrue(smallEpsilonSameCount < largeEpsilonSameCount);
    }

    protected void testDistribution(EncodeLdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<String, Long> histogramMap = IntStream.range(0, ROUND)
            .mapToObj(index -> mechanism.randomize(INPUT_VALUE))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String histogram = Arrays.toString(
            histogramMap.keySet().stream()
                .sorted()
                .map(noiseValue -> noiseValue + ": " + histogramMap.get(noiseValue))
                .toArray(String[]::new)
        );
        LOGGER.info("{}: {}", mechanism.getMechanismName(), histogram);
    }

    protected void testReseed(EncodeLdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            String[] round1s = IntStream.range(0, ROUND)
                .mapToObj(index -> mechanism.randomize(INPUT_VALUE))
                .toArray(String[]::new);
            mechanism.reseed(0L);
            String[] round2s = IntStream.range(0, ROUND)
                .mapToObj(index -> mechanism.randomize(INPUT_VALUE))
                .toArray(String[]::new);
            Assert.assertArrayEquals(round1s, round2s);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
