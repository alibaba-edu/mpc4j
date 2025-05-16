package edu.alibaba.mpc4j.common.tool.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * efficiency tests for block utilities.
 *
 * @author Weiran Liu
 * @date 2025/1/8
 */
public class BlockUtilsEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockUtilsEfficiencyTest.class);
    /**
     * round
     */
    private static final int ROUND = 1 << 22;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BlockUtilsEfficiencyTest() {
        secureRandom = new SecureRandom();
        LOGGER.info("{}\t{}\t{}", "      type", "     NAIVE", "    UNSAFE");
    }

    @Test
    public void testInplaceEfficiency() {
        StopWatch stopWatch = new StopWatch();
        byte[][] xs = BlockUtils.randomBlocks(ROUND, secureRandom);
        byte[][] ys = BlockUtils.randomBlocks(ROUND, secureRandom);
        double bytesTime, blockTime;

        // XORI
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.naiveXori(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.naiveXori(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.unsafeXori(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.unsafeXori(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      xori", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );

        // ANDI
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.naiveAndi(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.naiveAndi(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.unsafeAndi(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.unsafeAndi(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      andi", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );

        // ORI
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.naiveOri(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.naiveOri(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.unsafeOri(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.unsafeOri(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      ori ", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );

        // accumulate
        int num = 1 << 22;
        byte[][] data = BlockUtils.randomBlocks(num, secureRandom);
        stopWatch.start();
        byte[] expect = BlockUtils.zeroBlock();
        for (int i = 0; i < num; i++) {
            BlockUtils.xori(expect, data[i]);
        }
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        stopWatch.start();
        long[] actual = BlockUtils.zeroLongBlock();
        for (int i = 0; i < num; i++) {
            BlockUtils.xori(actual, data[i]);
        }
        BlockUtils.toByteArray(actual);
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      acc ", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );
    }
}
