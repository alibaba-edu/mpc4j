package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OKVS efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class OkvsEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OkvsEfficiencyTest.class);
    /**
     * default L
     */
    private static final int DEFAULT_L = CommonConstants.STATS_BIT_LENGTH;
    /**
     * default byte L
     */
    private static final int DEFAULT_BYTE_L = CommonUtils.getByteLength(DEFAULT_L);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * types
     */
    private static final OkvsType[] TYPES = new OkvsType[]{
        // ignore polynomial OKVS because it is too inefficient
        OkvsType.GBF,
        OkvsType.MEGA_BIN,
        OkvsType.H2_TWO_CORE_GCT,
        OkvsType.H2_DFS_GCT,
        OkvsType.H2_SINGLETON_GCT,
        OkvsType.H3_SINGLETON_GCT,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "      logN", " encode(s)", " decode(s)");
        testEfficiency(8);
        testEfficiency(10);
        testEfficiency(12);
        testEfficiency(14);
        testEfficiency(16);
        testEfficiency(18);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (OkvsType type : TYPES) {
            int hashNum = OkvsFactory.getHashNum(type);
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(EnvType.STANDARD, type, n, DEFAULT_L, keys);
            Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(n);
            // encode
            STOP_WATCH.start();
            byte[][] storage = okvs.encode(keyValueMap);
            STOP_WATCH.stop();
            double encodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            // decode
            STOP_WATCH.start();
            keyValueMap.keySet().forEach(key -> okvs.decode(storage, key));
            STOP_WATCH.stop();
            double decodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(encodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(decodeTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    private Map<ByteBuffer, byte[]> randomKeyValueMap(int size) {
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            byte[] keyBytes = new byte[DEFAULT_BYTE_L];
            SECURE_RANDOM.nextBytes(keyBytes);
            byte[] valueBytes = new byte[DEFAULT_BYTE_L];
            SECURE_RANDOM.nextBytes(valueBytes);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), valueBytes);
        });
        return keyValueMap;
    }
}
