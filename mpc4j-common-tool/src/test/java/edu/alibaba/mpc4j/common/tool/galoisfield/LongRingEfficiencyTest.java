package edu.alibaba.mpc4j.common.tool.galoisfield;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory.Zl64Type;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory.Zp64Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LongRing efficiency tests.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
@Ignore
public class LongRingEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongRingEfficiencyTest.class);
    /**
     * random num
     */
    private static final int MAX_RANDOM = 1 << 20;
    /**
     * the time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * the Zl64 test types
     */
    private static final Zl64Type[] ZL64_TYPES = new Zl64Type[]{Zl64Type.JDK, Zl64Type.RINGS,};
    /**
     * the Zp64 test types
     */
    private static final Zp64Type[] ZP64_TYPES = new Zp64Type[]{Zp64Type.RINGS};

    @Test
    public void testZl64Efficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "         l",
            "   add(us)", "   neg(us)", "   sub(us)", "   mul(us)", "   pow(us)"
        );
        int[] ls = new int[]{1, 2, 3, 4, 40, 62};
        for (int l : ls) {
            for (Zl64Type type : ZL64_TYPES) {
                Zl64 zl64 = Zl64Factory.createInstance(EnvType.STANDARD, type, l);
                testEfficiency(zl64);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    @Test
    public void testZp64Efficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "         l",
            "   add(us)", "   neg(us)", "   sub(us)", "   mul(us)", "   pow(us)"
        );
        int[] ls = new int[]{1, 2, 3, 4, 40, 62};
        for (int l : ls) {
            for (Zp64Type type : ZP64_TYPES) {
                Zp64 zp64 = Zp64Factory.createInstance(EnvType.STANDARD, type, l);
                testEfficiency(zp64);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(LongRing longRing) {
        int l = longRing.getL();
        // create random elements
        long[] arrayA = new long[MAX_RANDOM];
        long[] arrayB = new long[MAX_RANDOM];
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            arrayA[index] = longRing.createNonZeroRandom(SECURE_RANDOM);
            arrayB[index] = longRing.createNonZeroRandom(SECURE_RANDOM);
        });
        // warmup
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            longRing.add(arrayA[index], arrayB[index]);
            longRing.neg(arrayA[index]);
            longRing.sub(arrayA[index], arrayB[index]);
            longRing.mul(arrayA[index], arrayB[index]);
            longRing.pow(arrayA[index], arrayB[index]);
        });
        // add
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longRing.add(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double addTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // neg
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longRing.neg(arrayA[index]));
        STOP_WATCH.stop();
        double negTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // sub
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longRing.sub(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double subTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // mul
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longRing.mul(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double mulTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // pow
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longRing.pow(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double powTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(longRing.getName(), 20),
            StringUtils.leftPad(String.valueOf(l), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(addTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(negTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(subTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(powTime), 10)
        );
    }
}
