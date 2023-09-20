package edu.alibaba.mpc4j.common.tool.galoisfield;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;
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
public class BytesFieldEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BytesFieldEfficiencyTest.class);
    /**
     * random num
     */
    private static final int MAX_RANDOM = 1 << 16;
    /**
     * the time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();

    @Test
    public void testGf64Efficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}",
            "                type", "         l", "   mul(us)", "   div(us)", "   inv(us)"
        );
        for (Gf64Type type : Gf64Type.values()) {
            Gf64 gf64 = Gf64Factory.createInstance(EnvType.STANDARD, type);
            testEfficiency(gf64);
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testGf2kEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}",
            "                type", "         l", "   mul(us)", "   div(us)", "   inv(us)"
        );
        for (Gf2kType type : Gf2kType.values()) {
            Gf2k gf2k = Gf2kFactory.createInstance(EnvType.STANDARD, type);
            testEfficiency(gf2k);
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testGf2eEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}",
            "                type", "         l", "   mul(us)", "   div(us)", "   inv(us)"
        );
        int[] ls = new int[]{1, 2, 3, 4, 40, 62, 128};
        for (int l : ls) {
            for (Gf2eType type : Gf2eType.values()) {
                Gf2e gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, type, l);
                testEfficiency(gf2e);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(BytesField bytesField) {
        int l = bytesField.getL();
        // create random elements
        byte[][] arrayA = new byte[MAX_RANDOM][];
        byte[][] arrayB = new byte[MAX_RANDOM][];
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            arrayA[index] = bytesField.createNonZeroRandom(SECURE_RANDOM);
            arrayB[index] = bytesField.createNonZeroRandom(SECURE_RANDOM);
        });
        // warmup
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            bytesField.mul(arrayA[index], arrayB[index]);
            bytesField.div(arrayA[index], arrayB[index]);
            bytesField.inv(arrayA[index]);
        });
        // mul
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesField.mul(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double mulTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // div
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesField.div(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double divTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // inv
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesField.inv(arrayA[index]));
        STOP_WATCH.stop();
        double invTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(bytesField.getName(), 20),
            StringUtils.leftPad(String.valueOf(l), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(divTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(invTime), 10)
        );
    }
}
