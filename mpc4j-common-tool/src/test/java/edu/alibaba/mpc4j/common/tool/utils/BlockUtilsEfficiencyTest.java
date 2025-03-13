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
@SuppressWarnings("ResultOfMethodCallIgnored")
public class BlockUtilsEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockUtilsEfficiencyTest.class);
    /**
     * round
     */
    private static final int ROUND = 10000000;
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
        LOGGER.info("{}\t{}\t{}", "      type", "    byte[]", "     block");
    }

    @Test
    public void testInplaceEfficiency() {
        StopWatch stopWatch = new StopWatch();
        byte[][] xs = BytesUtils.randomByteArrayVector(ROUND, BlockUtils.BYTE_LENGTH, secureRandom);
        byte[][] ys = BytesUtils.randomByteArrayVector(ROUND, BlockUtils.BYTE_LENGTH, secureRandom);
        double bytesTime, blockTime;

        // XOR
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.xor(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.xor(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.xor(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.xor(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      xor ", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );

        // XORI
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.xori(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.xori(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.xori(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.xori(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      xori", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );

        // AND
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.and(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.and(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.and(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.and(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      and ", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );

        // ANDI
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.andi(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BytesUtils.andi(xs[i], ys[i]));
        stopWatch.stop();
        bytesTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS)  / ROUND;
        stopWatch.reset();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.andi(xs[i], ys[i]));
        stopWatch.start();
        IntStream.range(0, ROUND).forEach(i -> BlockUtils.andi(xs[i], ys[i]));
        stopWatch.stop();
        blockTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / ROUND;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("      andi", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bytesTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(blockTime), 10)
        );
    }
}
