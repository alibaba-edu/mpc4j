package edu.alibaba.mpc4j.common.tool.galoisfield;

import edu.alibaba.mpc4j.common.tool.EnvType;
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
public class LongFieldEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongFieldEfficiencyTest.class);
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
     * the Zp64 test types
     */
    private static final Zp64Type[] ZP64_TYPES = new Zp64Type[]{Zp64Type.RINGS};

    @Test
    public void testZp64Efficiency() {
        LOGGER.info("{}\t{}\t{}\t{}",
            "                type", "         l", "   div(us)", "   inv(us)"
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

    private void testEfficiency(LongField longField) {
        int l = longField.getL();
        // create random elements
        long[] arrayA = new long[MAX_RANDOM];
        long[] arrayB = new long[MAX_RANDOM];
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            arrayA[index] = longField.createNonZeroRandom(SECURE_RANDOM);
            arrayB[index] = longField.createNonZeroRandom(SECURE_RANDOM);
        });
        // warmup
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            longField.div(arrayA[index], arrayB[index]);
            longField.inv(arrayA[index]);
        });
        // div
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longField.div(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double divTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // inv
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> longField.inv(arrayA[index]));
        STOP_WATCH.stop();
        double invTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
            "{}\t{}\t{}\t{}",
            StringUtils.leftPad(longField.getName(), 20),
            StringUtils.leftPad(String.valueOf(l), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(divTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(invTime), 10)
        );
    }
}
