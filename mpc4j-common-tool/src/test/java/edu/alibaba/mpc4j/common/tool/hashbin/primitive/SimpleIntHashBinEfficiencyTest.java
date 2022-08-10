package edu.alibaba.mpc4j.common.tool.hashbin.primitive;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 简单整数哈希桶性能测试。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
@Ignore
public class SimpleIntHashBinEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleIntHashBinEfficiencyTest.class);
    /**
     * 数量输出格式
     */
    private static final DecimalFormat NUM_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 哈希数量
     */
    private final int[] HASH_NUMS = new int[]{1, 2, 3};

    @Test
    public void testEfficiency() {
        LOGGER.info("  hash_num\t    log(n)\t insert(s)\t    pad(s)");
        testEfficiency(14);
        testEfficiency(16);
        testEfficiency(18);
        testEfficiency(20);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (int hashNum : HASH_NUMS) {
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, HashBinTestUtils.SECURE_RANDOM);
            // 桶数量与元素数量一致，近似等于对应CuckooHash的要求
            SimpleIntHashBin intHashBin = new SimpleIntHashBin(EnvType.STANDARD, n, n, keys);
            int[] items = IntStream.range(0, n).toArray();
            // 插入元素
            STOP_WATCH.start();
            intHashBin.insertItems(items);
            STOP_WATCH.stop();
            double time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}",
                StringUtils.leftPad(String.valueOf(hashNum), 10),
                StringUtils.leftPad(NUM_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
