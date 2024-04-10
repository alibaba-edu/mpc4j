package edu.alibaba.mpc4j.common.tool.hashbin.object;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 随机填充哈希桶性能测试。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
@Ignore
public class RandomPadHashBinEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomPadHashBinEfficiencyTest.class);
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
    private static final int[] HASH_NUMS = new int[]{1, 2, 3};

    @Test
    public void testEfficiency() {
        LOGGER.info("  hash_num\t    log(n)\t   data(B)\t insert(s)\t    pad(s)\t Memory(B)");
        testEfficiency(14);
        testEfficiency(16);
        testEfficiency(18);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (int hashNum : HASH_NUMS) {
            List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(n);
            long dataMemory = GraphLayout.parseInstance(items).totalSize();
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, HashBinTestUtils.SECURE_RANDOM);
            // 桶数量与元素数量一致，近似等于对应CuckooHash的要求
            RandomPadHashBin<ByteBuffer> hashBin = new RandomPadHashBin<>(EnvType.STANDARD, n, n, keys);
            // 插入元素
            STOP_WATCH.start();
            hashBin.insertItems(items);
            STOP_WATCH.stop();
            double insertTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            // bin memory
            long binMemory = GraphLayout.parseInstance(hashBin).totalSize();
            // 填充元素
            STOP_WATCH.start();
            hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
            STOP_WATCH.stop();
            double randomPadTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(String.valueOf(hashNum), 10),
                StringUtils.leftPad(NUM_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(String.valueOf(dataMemory), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(insertTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(randomPadTime), 10),
                StringUtils.leftPad(String.valueOf(binMemory), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
