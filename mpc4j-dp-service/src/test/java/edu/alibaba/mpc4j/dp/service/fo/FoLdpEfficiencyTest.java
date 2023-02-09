package edu.alibaba.mpc4j.dp.service.fo;

import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Frequency Oracle LDP efficiency test.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
@Ignore
public class FoLdpEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoLdpEfficiencyTest.class);
    /**
     * server stop watch
     */
    private static final StopWatch SERVER_STOP_WATCH = new StopWatch();
    /**
     * client stop watch
     */
    private static final StopWatch CLIENT_STOP_WATCH = new StopWatch();
    /**
     * ε output format
     */
    private static final DecimalFormat EPSILON_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * time output format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * int format
     */
    private static final DecimalFormat INTEGER_DECIMAL_FORMAT = new DecimalFormat("0");
    /**
     * ε array
     */
    private static final double[] EPSILONS = new double[] {1, 2, 4, 8, 16};
    /**
     * the type
     */
    private static final FoLdpType[] TYPES = new FoLdpType[] {
        // Apple's Hadamard Count Mean Sketch
        FoLdpType.APPLE_HCMS,
        // Apple's Count Mean Sketch
        FoLdpType.APPLE_CMS,
        // Hadamard Mechanism with low ε
        FoLdpType.HM_LOW_EPSILON,
        // Hadamard Mechanism
        FoLdpType.HM,
        // Hadamard Response with high ε
        FoLdpType.HR_HIGH_EPSILON,
        // Hadamard Response
        FoLdpType.HR,
        // Fast Local Hash
        FoLdpType.FLH,
        // Optimal Local Hash
        FoLdpType.OLH,
        // Binary Local Hash
        FoLdpType.BLH,
        // RAPPOR
        FoLdpType.RAPPOR,
        // Optimized Unary Encoding
        FoLdpType.OUE,
        // Symmetric Unary Encoding
        FoLdpType.SUE,
        // Direct Encoding via Index Encoding
        FoLdpType.DE_INDEX,
        // Direct Encoding via String Encoding
        FoLdpType.DE_STRING,
    };

    public FoLdpEfficiencyTest() {
        // empty
    }

    @Test
    public void testEfficiency() throws IOException {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                name", "         ε", "            variance",
            " s_time(s)", " c_time(s)", "  comm.(B)", "   mem.(B)"
        );
        for (double epsilon : EPSILONS) {
            testEfficiency(epsilon);
        }
    }

    private void testEfficiency(double epsilon) throws IOException {
        for (FoLdpType type : TYPES) {
            FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.CONNECT_DATA_DOMAIN, epsilon);
            // create server and client
            FoLdpServer server = FoLdpFactory.createServer(config);
            FoLdpClient client = FoLdpFactory.createClient(config);
            // randomize
            SERVER_STOP_WATCH.start();
            SERVER_STOP_WATCH.suspend();
            CLIENT_STOP_WATCH.start();
            CLIENT_STOP_WATCH.suspend();
            Random ldpRandom = new Random();
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.CONNECT_DATA_PATH);
            long payloadBytes = dataStream
                .mapToLong(item -> {
                    CLIENT_STOP_WATCH.resume();
                    byte[] itemBytes = client.randomize(item, ldpRandom);
                    CLIENT_STOP_WATCH.suspend();
                    SERVER_STOP_WATCH.resume();
                    server.insert(itemBytes);
                    SERVER_STOP_WATCH.suspend();
                    return itemBytes.length;
                })
                .sum();
            dataStream.close();
            Map<String, Double> estimates = server.estimate();
            double variance = LdpTestDataUtils.getVariance(estimates, LdpTestDataUtils.CORRECT_CONNECT_COUNT_MAP);
            // server time
            SERVER_STOP_WATCH.stop();
            double serverTime = (double) SERVER_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            SERVER_STOP_WATCH.reset();
            // client time
            CLIENT_STOP_WATCH.stop();
            double clientTime = (double) CLIENT_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            CLIENT_STOP_WATCH.reset();
            long memory = GraphLayout.parseInstance(server).totalSize();
            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(EPSILON_DECIMAL_FORMAT.format(epsilon), 10),
                StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(variance), 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(serverTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(clientTime), 10),
                StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(payloadBytes), 10),
                StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(memory), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
