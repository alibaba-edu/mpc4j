package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * HeavyGuardian-based Heavy Hitter LDP service tests.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
@RunWith(Parameterized.class)
public class HgHhLdpTest {
    /**
     * large ε
     */
    private static final double LARGE_EPSILON = 128;
    /**
     * default ε
     */
    private static final double DEFAULT_EPSILON = 16;
    /**
     * default k, since HeavyGuardian has random state, here we only require k = 10 heavy hitters.
     */
    private static final int DEFAULT_K = 10;
    /**
     * λ_h
     */
    private static final int LAMBDA_H = DEFAULT_K * 2;
    /**
     * w = 1
     */
    private static final int W1 = 1;
    /**
     * λ_h for w = 1
     */
    private static final int W1_LAMBDA_H = (int) Math.ceil((double) LAMBDA_H / W1);
    /**
     * w = 3
     */
    private static final int W3 = 3;
    /**
     * λ_h for w = 3
     */
    private static final int W3_LAMBDA_H = (int) Math.ceil((double) LAMBDA_H / W3);
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CNR
        configurations.add(new Object[]{HhLdpFactory.HhLdpType.CNR.name(), HhLdpFactory.HhLdpType.CNR,});
        // BDR
        configurations.add(new Object[]{HhLdpFactory.HhLdpType.BDR.name(), HhLdpFactory.HhLdpType.BDR,});
        // DSR
        configurations.add(new Object[]{HhLdpFactory.HhLdpType.DSR.name(), HhLdpFactory.HhLdpType.DSR,});
        // BGR
        configurations.add(new Object[]{HhLdpFactory.HhLdpType.BGR.name(), HhLdpFactory.HhLdpType.BGR,});

        return configurations;
    }

    /**
     * the type
     */
    private final HhLdpFactory.HhLdpType type;

    public HgHhLdpTest(String name, HhLdpFactory.HhLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testW1Warmup() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON, W1, W1_LAMBDA_H, hgRandom
        );
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        testWarmup(server, client);
    }

    @Test
    public void testW3Warmup() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON, W3, W3_LAMBDA_H, hgRandom
        );
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        testWarmup(server, client);
    }

    private void testWarmup(HhLdpServer server, HhLdpClient client) throws IOException {
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.map(client::warmup).forEach(server::warmupInsert);
        dataStream.close();
        // get heavy hitters
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertEquals(DEFAULT_K, heavyHitters.size());
        List<Map.Entry<String, Integer>> correctHeavyHitters = LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_ORDERED_LIST;
        List<Map.Entry<String, Double>> orderedHeavyHitters = server.orderedHeavyHitters();
        Assert.assertEquals(DEFAULT_K, orderedHeavyHitters.size());
        // verify top k are correct
        for (int index = 0; index < DEFAULT_K; index++) {
            Assert.assertEquals(correctHeavyHitters.get(index).getKey(), orderedHeavyHitters.get(index).getKey());
        }
    }

    @Test
    public void testW1StopWarmup() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON, W1, W1_LAMBDA_H, hgRandom
        );
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        testStopWarmup(server, client);
    }

    @Test
    public void testW3StopWarmup() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON, W3, W3_LAMBDA_H, hgRandom
        );
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        testStopWarmup(server, client);
    }

    private void testStopWarmup(HhLdpServer server, HhLdpClient client) throws IOException {
        // warmup
        StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        server.stopWarmup();
        // get heavy hitters
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertEquals(DEFAULT_K, heavyHitters.size());
        List<Map.Entry<String, Integer>> correctHeavyHitters = LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_ORDERED_LIST;
        List<Map.Entry<String, Double>> orderedHeavyHitters = server.orderedHeavyHitters();
        Assert.assertEquals(DEFAULT_K, orderedHeavyHitters.size());
        // verify no-error count
        for (int index = 0; index < DEFAULT_K; index++) {
            Assert.assertEquals(correctHeavyHitters.get(index).getKey(), orderedHeavyHitters.get(index).getKey());
        }
    }

    @Test
    public void testW1LargeEpsilon() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, LARGE_EPSILON, W1, W1_LAMBDA_H, hgRandom
        );
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        testLargeEpsilon(server, client);
    }

    @Test
    public void testW3LargeEpsilon() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, LARGE_EPSILON, W3, W3_LAMBDA_H, hgRandom
        );
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        testLargeEpsilon(server, client);
    }

    private void testLargeEpsilon(HhLdpServer server, HhLdpClient client) throws IOException {
        // warmup
        HhLdpTest.exampleWarmupInsert(server, client, LdpTestDataUtils.EXAMPLE_WARMUP_NUM);
        server.stopWarmup();
        // randomize
        HhLdpTest.exampleRandomizeInsert(server, client, LdpTestDataUtils.EXAMPLE_WARMUP_NUM);
        // get heavy hitters
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertEquals(DEFAULT_K, heavyHitters.size());
        List<Map.Entry<String, Integer>> correctHeavyHitters = LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_ORDERED_LIST;
        List<Map.Entry<String, Double>> orderedHeavyHitters = server.orderedHeavyHitters();
        Assert.assertEquals(DEFAULT_K, orderedHeavyHitters.size());
        // verify no-error count
        for (int index = 0; index < DEFAULT_K; index++) {
            Assert.assertEquals(correctHeavyHitters.get(index).getKey(), orderedHeavyHitters.get(index).getKey());
        }
    }

    @Test
    public void testW1LargeEpsilonWithoutWarmup() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, LARGE_EPSILON, W1, W1_LAMBDA_H, hgRandom
        );
        if (!(config instanceof HhgHhLdpConfig)) {
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            testLargeEpsilonWithoutWarmup(server, client);
        }
    }

    @Test
    public void testW3LargeEpsilonWithoutWarmup() throws IOException {
        Random hgRandom = new Random();
        HgHhLdpConfig config = HhLdpFactory.createDefaultHgHhLdpConfig(
            type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, LARGE_EPSILON, W3, W3_LAMBDA_H, hgRandom
        );
        if (!(config instanceof HhgHhLdpConfig)) {
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            testLargeEpsilonWithoutWarmup(server, client);
        }
    }

    private void testLargeEpsilonWithoutWarmup(HhLdpServer server, HhLdpClient client) throws IOException {
        // warmup
        HhLdpTest.exampleWarmupInsert(server, client, 0);
        server.stopWarmup();
        // randomize
        HhLdpTest.exampleRandomizeInsert(server, client, 0);
        // get heavy hitters
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertEquals(DEFAULT_K, heavyHitters.size());
        List<Map.Entry<String, Integer>> correctHeavyHitters = LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_ORDERED_LIST;
        List<Map.Entry<String, Double>> orderedHeavyHitters = server.orderedHeavyHitters();
        Assert.assertEquals(DEFAULT_K, orderedHeavyHitters.size());
        // verify no-error count
        for (int index = 0; index < DEFAULT_K; index++) {
            Assert.assertEquals(correctHeavyHitters.get(index).getKey(), orderedHeavyHitters.get(index).getKey());
        }
    }
}
