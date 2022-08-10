package edu.alibaba.mpc4j.common.tool.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
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
 * 过滤器性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class FilterEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEfficiencyTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 最大元素数量对数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final FilterType[] TYPES = new FilterType[] {
        FilterType.SET_FILTER,
        FilterType.BLOOM_FILTER,
        FilterType.SPARSE_BLOOM_FILTER,
        FilterType.CUCKOO_FILTER,
        FilterType.VACUUM_FILTER,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                name", "    log(n)", "   time(s)");
        // 2^4个元素
        testEfficiency(4);
        // 2^8个元素
        testEfficiency(8);
        // 2^12个元素
        testEfficiency(12);
        // 2^16个元素
        testEfficiency(16);
        // 2^20个元素
        testEfficiency(20);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (FilterType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, n), SECURE_RANDOM);
            List<byte[]> items = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] item = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(item);
                    return item;
                })
                .collect(Collectors.toList());
            Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, n, keys);
            STOP_WATCH.start();
            items.forEach(item -> filter.put(ByteBuffer.wrap(item)));
            STOP_WATCH.stop();
            double time = (double)STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
