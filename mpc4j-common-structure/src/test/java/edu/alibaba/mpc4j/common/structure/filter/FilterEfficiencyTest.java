package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Filter efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class FilterEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEfficiencyTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * types
     */
    private static final FilterType[] TYPES = new FilterType[]{
        FilterType.SET_FILTER,
        FilterType.NAIVE_RANDOM_BLOOM_FILTER,
        FilterType.SPARSE_RANDOM_BLOOM_FILTER,
        FilterType.DISTINCT_BLOOM_FILTER,
        FilterType.CUCKOO_FILTER,
        FilterType.VACUUM_FILTER,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                          name", "    log(n)", "   time(s)");
        testEfficiency(4);
        testEfficiency(8);
        testEfficiency(12);
        testEfficiency(16);
        testEfficiency(20);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (FilterType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
            List<byte[]> items = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] item = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(item);
                    return item;
                })
                .collect(Collectors.toList());
            Filter<ByteBuffer> filter = FilterFactory.load(EnvType.STANDARD, type, n, keys);
            STOP_WATCH.start();
            items.forEach(item -> filter.put(ByteBuffer.wrap(item)));
            STOP_WATCH.stop();
            double time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
