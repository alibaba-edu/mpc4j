package edu.alibaba.mpc4j.dp.cdp.nominal;

import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 枚举型CDP机制测试抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
abstract class AbstractNominalCdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNominalCdpTest.class);
    /**
     * 输出格式
     */
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 测试轮数
     */
    protected static final int ROUND = 1 << 14;
    /**
     * 默认输入
     */
    protected static final String INPUT_VALUE = "A";
    /**
     * 默认评分函数映射
     */
    protected final ArrayList<NounPairDistance> defaultNounPairDistances;

    protected AbstractNominalCdpTest() {
        defaultNounPairDistances = new ArrayList<>();
        defaultNounPairDistances.add(NounPairDistance.createFromNouns("A", "B", 1.0));
        defaultNounPairDistances.add(NounPairDistance.createFromNouns("A", "C", 2.0));
        defaultNounPairDistances.add(NounPairDistance.createFromNouns("B", "C", 2.0));
    }

    protected void testFunctionality(NominalCdp mechanism) {
        LOGGER.info("-----test functionality-----");
        LOGGER.info("{}: A -> {}", mechanism.getMechanismName(), mechanism.randomize("A"));

    }

    protected void testLargeEpsilon(NominalCdp mechanism) {
        LOGGER.info("-----test large ε-----");
        // ε无穷大时，随机结果应该都相同
        IntStream.range(0, ROUND)
            .mapToObj(index -> mechanism.randomize("A"))
            .forEach(noiseValue -> Assert.assertEquals(noiseValue, "A"));
        LOGGER.info("{}: A -> {}", mechanism.getMechanismName(), mechanism.randomize("A"));
    }

    protected void testEpsilon(NominalCdp smallMechanism, NominalCdp largeMechanism) {
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

    protected void testDistribution(NominalCdp mechanism) {
        LOGGER.info("-----test distribution-----");
        Map<String, Long> resultCount = IntStream.range(0, ROUND)
            .mapToObj(i -> mechanism.randomize("A"))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long countA = resultCount.get("A");
        long countB = resultCount.get("B");
        long countC = resultCount.get("C");
        double a2a = (double) countA / ROUND;
        double a2b = (double) countB / ROUND;
        double a2c = (double) countC / ROUND;
        LOGGER.info("{}: Pr[A -> A] = {}, Pr[A -> B] = {}, Pr[A -> C] = {}", mechanism.getMechanismName(),
            DECIMAL_FORMAT.format(a2a),
            DECIMAL_FORMAT.format(a2b),
            DECIMAL_FORMAT.format(a2c)
        );
        Assert.assertTrue(a2a <= Math.exp(2 * mechanism.getDeltaQ() * mechanism.getEpsilon()) * countC / ROUND + 0.05);
        // A -> A的概率因子是0，A -> B的概率因子是1，是A -> C的概率因子是2，因此A/B和C/B的数量应该是近似相等的
        Assert.assertTrue(Precision.equals(((double) countB) / ((double) countA), ((double) countC) / ((double) countB), 0.2));
    }

    protected void testReseed(NominalCdp mechanism) {
        LOGGER.info("-----test reseed-----");
        try {
            mechanism.reseed(0L);
            String[] round1s = IntStream.range(0, ROUND)
                .mapToObj(index -> mechanism.randomize("A"))
                .toArray(String[]::new);
            mechanism.reseed(0L);
            String[] round2s = IntStream.range(0, ROUND)
                .mapToObj(index -> mechanism.randomize("A"))
                .toArray(String[]::new);
            Assert.assertArrayEquals(round1s, round2s);
            LOGGER.info("{} reseed outputs same results", mechanism.getMechanismName());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("{} does not support reseed", mechanism.getMechanismName());
        }
    }
}
