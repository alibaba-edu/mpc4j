package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.common.tool.metrics.HeavyHitterMetrics;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.*;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Heavy Hitter LDP efficiency test.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
@Ignore
public class HhLdpEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HhLdpEfficiencyTest.class);
    /**
     * default k
     */
    private static final int DEFAULT_K = 20;
    /**
     * server stop watch
     */
    private static final StopWatch SERVER_STOP_WATCH = new StopWatch();
    /**
     * client stop watch
     */
    private static final StopWatch CLIENT_STOP_WATCH = new StopWatch();
    /**
     * double output format
     */
    private static final DecimalFormat DOUBLE_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * time output format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0");
    /**
     * int format
     */
    private static final DecimalFormat INTEGER_DECIMAL_FORMAT = new DecimalFormat("0");
    /**
     * ε array
     */
    private static final double[] EPSILONS = new double[]{1, 2, 4, 8, 16};
    /**
     * the type
     */
    private static final ArrayList<HhLdpConfig> CONFIGS;

    static  {
        CONFIGS = new ArrayList<>();
        for (double epsilon : EPSILONS) {
            // Apple Hadamard Count Mean Sketch
            FoLdpConfig appleHcmsFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.APPLE_HCMS, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(appleHcmsFoLdpConfig, DEFAULT_K).build());
            // Apple Count Mean Sketch
            FoLdpConfig appleCmsFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.APPLE_CMS, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(appleCmsFoLdpConfig, DEFAULT_K).build());
            // Hadamard Mechanism
            FoLdpConfig hmFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.HM, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(hmFoLdpConfig, DEFAULT_K).build());
            // Hadamard Response with high ε
            FoLdpConfig hrHighEpsilonFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.HR_HIGH_EPSILON, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(hrHighEpsilonFoLdpConfig, DEFAULT_K).build());
            // Hadamard Response
            FoLdpConfig hrFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.HR, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(hrFoLdpConfig, DEFAULT_K).build());
            // Optimal Local Hash
            FoLdpConfig olhFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.OLH, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(olhFoLdpConfig, DEFAULT_K).build());
            // Binary Local Hash
            FoLdpConfig blhFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.BLH, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(blhFoLdpConfig, DEFAULT_K).build());
            // RAPPOR
            FoLdpConfig rapporFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.RAPPOR, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(rapporFoLdpConfig, DEFAULT_K).build());
            // Optimized Unary Encoding
            FoLdpConfig oueFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.OUE, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(oueFoLdpConfig, DEFAULT_K).build());
            // Symmetric Unary Encoding
            FoLdpConfig sueFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.SUE, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(sueFoLdpConfig, DEFAULT_K).build());
            // Direct Encoding via Index Encoding
            FoLdpConfig deIndexFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.DE_INDEX, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(deIndexFoLdpConfig, DEFAULT_K).build());
            // Direct Encoding via String Encoding
            FoLdpConfig deStringFoLdpConfig = FoLdpFactory.createDefaultConfig(
                FoLdpType.DE_STRING, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon
            );
            CONFIGS.add(new FoHhLdpConfig.Builder(deStringFoLdpConfig, DEFAULT_K).build());
            // BGR
            CONFIGS.add(
                new BgrHgHhLdpConfig
                    .Builder(LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_K, epsilon)
                    .build()
            );
            // DSR
            CONFIGS.add(
                new DsrHgHhLdpConfig
                    .Builder(LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_K, epsilon)
                    .build()
            );
            // BDR
            CONFIGS.add(
                new BdrHhgHhLdpConfig
                    .Builder(LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_K, epsilon)
                    .build()
            );
            // CNR
            CONFIGS.add(
                new CnrHhgHhLdpConfig
                    .Builder(LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_K, epsilon)
                    .build()
            );
        }
    }

    public HhLdpEfficiencyTest() {
        // empty
    }

    @Test
    public void testEfficiency() throws IOException {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", "                name",
            "         ε", " s_time(s)", " c_time(s)", "  comm.(B)", "   mem.(B)",
            "      ndcg", " precision", "       abe", "        re"
            );
        for (HhLdpConfig config : CONFIGS) {
            String name = config.getName();
            // create server and client
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            // warmup
            AtomicInteger warmupIndex = new AtomicInteger();
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.CONNECT_DATA_PATH);
            dataStream.filter(item -> warmupIndex.getAndIncrement() <= LdpTestDataUtils.CONNECT_WARMUP_NUM)
                .map(client::warmup)
                .forEach(server::warmupInsert);
            dataStream.close();
            server.stopWarmup();
            // randomize
            SERVER_STOP_WATCH.start();
            SERVER_STOP_WATCH.suspend();
            CLIENT_STOP_WATCH.start();
            CLIENT_STOP_WATCH.suspend();
            Random ldpRandom = new Random();
            AtomicInteger randomizedIndex = new AtomicInteger();
            dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.CONNECT_DATA_PATH);
            long payloadBytes = dataStream
                .filter(item -> randomizedIndex.getAndIncrement() > LdpTestDataUtils.CONNECT_WARMUP_NUM)
                .mapToLong(item -> {
                    CLIENT_STOP_WATCH.resume();
                    byte[] itemBytes = client.randomize(server.getServerContext(), item, ldpRandom);
                    CLIENT_STOP_WATCH.suspend();
                    SERVER_STOP_WATCH.resume();
                    server.randomizeInsert(itemBytes);
                    SERVER_STOP_WATCH.suspend();
                    return itemBytes.length;
                })
                .sum();
            dataStream.close();
            // server time
            SERVER_STOP_WATCH.stop();
            double serverTime = (double) SERVER_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            SERVER_STOP_WATCH.reset();
            // client time
            CLIENT_STOP_WATCH.stop();
            double clientTime = (double) CLIENT_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            CLIENT_STOP_WATCH.reset();
            long memory = GraphLayout.parseInstance(server).totalSize();
            // result
            int k = config.getK();
            Map<String, Integer> expectHeavyHitterMap = LdpTestDataUtils.CORRECT_CONNECT_COUNT_ORDER_LIST
                .subList(0, k)
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            List<String> expectHeavyHitter = new ArrayList<>(expectHeavyHitterMap.keySet());
            Map<String, Double> actualHeavyHitterMap = server.heavyHitters();
            List<String> actualHeavyHitter = server.orderedHeavyHitters()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            double ndcg = HeavyHitterMetrics.ndcg(actualHeavyHitter, expectHeavyHitter);
            double precision = HeavyHitterMetrics.precision(actualHeavyHitter, expectHeavyHitter);
            double abe = HeavyHitterMetrics.absoluteError(actualHeavyHitterMap, expectHeavyHitterMap);
            double re = HeavyHitterMetrics.relativeError(actualHeavyHitterMap, expectHeavyHitterMap);
            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(name, 20),
                StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(config.getWindowEpsilon()), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(serverTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(clientTime), 10),
                StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(payloadBytes), 10),
                StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(memory), 10),
                StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(ndcg), 10),
                StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(precision), 10),
                StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(abe), 10),
                StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(re), 10)
            );
        }
    }
}
