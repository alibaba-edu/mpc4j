package edu.alibaba.mpc4j.dp.service.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Frequency Oracle LDP test.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
@RunWith(Parameterized.class)
public class FoLdpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoLdpTest.class);
    /**
     * large ε
     */
    static final double LARGE_EPSILON = 128;
    /**
     * default ε
     */
    static final double DEFAULT_EPSILON = 16;
    /**
     * the top-k ordered frequency estimate num
     */
    private static final int ORDERED_FREQUENCY_ESTIMATE_TOP_K = 10;
    /**
     * constant input
     */
    private static final String CONSTANT_INPUT = String.valueOf(0);
    /**
     * number of items for constant input
     */
    private static final int CONSTANT_INPUT_NUM = 100000;
    /**
     * constant input d = 2^k
     */
    private static final int CONSTANT_INPUT_POW_2_D = 1 << 5;
    /**
     * constant input d = 2^k + 1
     */
    private static final int CONSTANT_INPUT_POW_2_ADD_1_D = CONSTANT_INPUT_POW_2_D + 1;
    /**
     * constant input d = 2^k - 1
     */
    private static final int CONSTANT_INPUT_POW_2_SUB_1_D = CONSTANT_INPUT_POW_2_D - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Apple's Hadamard Count Mean Sketch
        configurations.add(new Object[]{FoLdpType.APPLE_HCMS.name(), FoLdpType.APPLE_HCMS,});
        // Apple's Count Mean Sketch
        configurations.add(new Object[]{FoLdpType.APPLE_CMS.name(), FoLdpType.APPLE_CMS,});
        // Hadamard Mechanism with low ε
        configurations.add(new Object[]{FoLdpType.HM_LOW_EPSILON.name(), FoLdpType.HM_LOW_EPSILON,});
        // Hadamard Mechanism
        configurations.add(new Object[]{FoLdpType.HM.name(), FoLdpType.HM,});
        // Hadamard Response with high ε
        configurations.add(new Object[]{FoLdpType.HR_HIGH_EPSILON.name(), FoLdpType.HR_HIGH_EPSILON,});
        // Hadamard Response
        configurations.add(new Object[]{FoLdpType.HR.name(), FoLdpType.HR,});
        // Fast Local Hash
        configurations.add(new Object[]{FoLdpType.FLH.name(), FoLdpType.FLH,});
        // Optimal Local Hash
        configurations.add(new Object[]{FoLdpType.OLH.name(), FoLdpType.OLH,});
        // Binary Local Hash
        configurations.add(new Object[]{FoLdpType.BLH.name(), FoLdpType.BLH,});
        // RAPPOR
        configurations.add(new Object[]{FoLdpType.RAPPOR.name(), FoLdpType.RAPPOR,});
        // Optimized Unary Encoding
        configurations.add(new Object[]{FoLdpType.OUE.name(), FoLdpType.OUE,});
        // Symmetric Unary Encoding
        configurations.add(new Object[]{FoLdpType.SUE.name(), FoLdpType.SUE,});
        // Direct Encoding via Index Encoding
        configurations.add(new Object[]{FoLdpType.DE_INDEX.name(), FoLdpType.DE_INDEX,});
        // Direct Encoding via String Encoding
        configurations.add(new Object[]{FoLdpType.DE_STRING.name(), FoLdpType.DE_STRING,});

        return configurations;
    }

    /**
     * the type
     */
    private final FoLdpType type;
    /**
     * large ε
     */
    private final double largeEpsilon;
    /**
     * default ε
     */
    private final double defaultEpsilon;

    public FoLdpTest(String name, FoLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        largeEpsilon = Math.min(FoLdpFactory.getMaximalEpsilon(type), LARGE_EPSILON);
        defaultEpsilon = Math.min(FoLdpFactory.getMaximalEpsilon(type), DEFAULT_EPSILON);
    }

    @Test
    public void testType() {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_EPSILON);
        // create server
        FoLdpServer server = FoLdpFactory.createServer(config);
        Assert.assertEquals(type, server.getType());
        // create client
        FoLdpClient client = FoLdpFactory.createClient(config);
        Assert.assertEquals(type, client.getType());
    }

    @Test
    public void testLargeEpsilonConstantInput() {
        testConstantInput(CONSTANT_INPUT_POW_2_D);
        testConstantInput(CONSTANT_INPUT_POW_2_ADD_1_D);
        testConstantInput(CONSTANT_INPUT_POW_2_SUB_1_D);
    }

    private void testConstantInput(int d) {
        Set<String> domain = IntStream.range(0, d)
            .mapToObj(String::valueOf)
            .collect(Collectors.toSet());
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, domain, largeEpsilon);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        Random ldpRandom = new Random();
        IntStream.range(0, CONSTANT_INPUT_NUM)
            .mapToObj(num -> client.randomize(CONSTANT_INPUT, ldpRandom))
            .forEach(server::insert);
        // verify the num
        Assert.assertEquals(CONSTANT_INPUT_NUM, server.getNum());
        // estimate
        Map<String, Double> frequencyEstimates = server.estimate();
        Assert.assertEquals(d, frequencyEstimates.size());
        Assert.assertEquals(CONSTANT_INPUT_NUM, frequencyEstimates.get(CONSTANT_INPUT), 0.01 * CONSTANT_INPUT_NUM);
    }

    @Test
    public void testLargeEpsilon() throws IOException {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, largeEpsilon);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> frequencyEstimates = server.estimate();
        Assert.assertEquals(LdpTestDataUtils.EXAMPLE_DATA_D, frequencyEstimates.size());
        if (FoLdpFactory.isConverge(type)) {
            for (String item : LdpTestDataUtils.EXAMPLE_DATA_DOMAIN) {
                // verify no-error count
                Assert.assertEquals(
                    LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP.get(item), frequencyEstimates.get(item), DoubleUtils.PRECISION
                );
            }
        } else {
            // there are some mechanism that do not provide un-biased sum even for large epsilon
            // for those mechanisms, the constant input test case would help to verify the un-biased estimation.
            // here we just print the top-10 results
            List<Map.Entry<String, Double>> orderedFrequencyEstimateList = server.orderedEstimate();
            StringBuilder expectOrderedStringBuilder = new StringBuilder();
            StringBuilder actualOrderedStringBuilder = new StringBuilder();
            for (int i = 0; i < ORDERED_FREQUENCY_ESTIMATE_TOP_K; i++) {
                Map.Entry<String, Integer> expectMap = LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_ORDERED_LIST.get(i);
                expectOrderedStringBuilder
                    .append(StringUtils.leftPad("(" + expectMap.getKey() + ", " + expectMap.getValue() + ")", 10))
                    .append("\t");
                Map.Entry<String, Double> actualMap = orderedFrequencyEstimateList.get(i);
                actualOrderedStringBuilder
                    .append(StringUtils.leftPad("(" + actualMap.getKey() + ", " + actualMap.getValue().intValue() + ")", 10))
                    .append("\t");
            }
            double estimateSum = frequencyEstimates.values().stream().mapToDouble(i -> i).sum();
            LOGGER.info("{}:\n expect sum = {}\n actual sum = {}\n expect order = {}\n actual order = {}",
                type.name(), LdpTestDataUtils.EXAMPLE_TOTAL_NUM, estimateSum,
                expectOrderedStringBuilder, actualOrderedStringBuilder
            );
        }
    }

    @Test
    public void testDefault() throws IOException {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, defaultEpsilon);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> frequencyEstimates = server.estimate();
        Assert.assertEquals(LdpTestDataUtils.EXAMPLE_DATA_D, frequencyEstimates.size());
        // compute the variance
        int totalNum = LdpTestDataUtils.EXAMPLE_TOTAL_NUM;
        double variance = LdpTestDataUtils.getVariance(frequencyEstimates, LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP);
        if (FoLdpFactory.isConverge(type)) {
            double averageVariance = variance / totalNum;
            Assert.assertTrue(averageVariance <= 1);
        } else {
            // there are some mechanisms that do not get accurate answer even for large epsilon
            // for those mechanisms, we only print its average variance
            double averageStdDev = Math.sqrt(variance) / totalNum;
            Assert.assertTrue(averageStdDev <= 1);
        }
    }

    private static void exampleRandomizeInsert(FoLdpServer server, FoLdpClient client) throws IOException {
        Random ldpRandom = new Random();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.map(item -> client.randomize(item, ldpRandom)).forEach(server::insert);
        dataStream.close();
    }

    @Test
    public void testLargeDomain() throws IOException {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_LARGE_DOMAIN, defaultEpsilon);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> frequencyEstimates = server.estimate();
        Assert.assertEquals(LdpTestDataUtils.EXAMPLE_LARGE_D, frequencyEstimates.size());
        // compute the variance
        int totalNum = LdpTestDataUtils.EXAMPLE_TOTAL_NUM;
        double variance = LdpTestDataUtils.getVariance(frequencyEstimates, LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP);
        if (FoLdpFactory.isConverge(type)) {
            double averageVariance = variance / totalNum;
            Assert.assertTrue(averageVariance <= 1);
        } else {
            // there are some mechanisms that do not get accurate answer even for large epsilon
            // for those mechanisms, we only print its average variance
            double averageStdDev = Math.sqrt(variance) / totalNum;
            Assert.assertTrue(averageStdDev <= 1);
        }
    }
}
