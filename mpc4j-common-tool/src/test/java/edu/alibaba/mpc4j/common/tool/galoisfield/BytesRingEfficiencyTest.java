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
 * BytesRing efficiency tests.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
@Ignore
public class BytesRingEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BytesRingEfficiencyTest.class);
    /**
     * random num
     */
    private static final int MAX_RANDOM = 1 << 16;
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

    @Test
    public void testGf64Efficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "         l",
            "   add(us)", "  addi(us)", "   neg(us)", "  negi(us)", "   sub(us)", "  subi(us)", "   mul(us)", "  muli(us)"
        );
        for (Gf64Type type : Gf64Type.values()) {
            Gf64 gf64 = Gf64Factory.createInstance(EnvType.STANDARD, type);
            testEfficiency(gf64);
        }
    }

    @Test
    public void testGf2kEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "         l",
            "   add(us)", "  addi(us)", "   neg(us)", "  negi(us)", "   sub(us)", "  subi(us)", "   mul(us)", "  muli(us)"
        );
        for (Gf2kType type : Gf2kType.values()) {
            Gf2k gf2k = Gf2kFactory.createInstance(EnvType.STANDARD, type);
            testEfficiency(gf2k);
        }
    }

    @Test
    public void testGf2eEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "         l",
            "   add(us)", "  addi(us)", "   neg(us)", "  negi(us)", "   sub(us)", "  subi(us)", "   mul(us)", "  muli(us)"
        );
        int[] ls = new int[]{1, 2, 3, 4, 40, 62, 128, 256};
        for (int l : ls) {
            for (Gf2eType type : Gf2eType.values()) {
                Gf2e gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, type, l);
                testEfficiency(gf2e);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(BytesRing bytesRing) {
        int l = bytesRing.getL();
        // create random elements
        byte[][] arrayA = new byte[MAX_RANDOM][];
        byte[][] arrayB = new byte[MAX_RANDOM][];
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            arrayA[index] = bytesRing.createNonZeroRandom(SECURE_RANDOM);
            arrayB[index] = bytesRing.createNonZeroRandom(SECURE_RANDOM);
        });
        // warmup
        IntStream.range(0, MAX_RANDOM).forEach(index -> {
            bytesRing.add(arrayA[index], arrayB[index]);
            bytesRing.addi(arrayA[index], arrayB[index]);
            bytesRing.neg(arrayA[index]);
            bytesRing.negi(arrayA[index]);
            bytesRing.sub(arrayA[index], arrayB[index]);
            bytesRing.subi(arrayA[index], arrayB[index]);
            bytesRing.mul(arrayA[index], arrayB[index]);
            bytesRing.muli(arrayA[index], arrayB[index]);
        });
        // add
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.add(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double addTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // addi
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.addi(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double addiTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // neg
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.neg(arrayA[index]));
        STOP_WATCH.stop();
        double negTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // negi
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.negi(arrayA[index]));
        STOP_WATCH.stop();
        double negiTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // sub
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.sub(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double subTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // subi
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.subi(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double subiTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // mul
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.mul(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double mulTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // muli
        STOP_WATCH.start();
        IntStream.range(0, MAX_RANDOM).forEach(index -> bytesRing.muli(arrayA[index], arrayB[index]));
        STOP_WATCH.stop();
        double muliTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_RANDOM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(bytesRing.getName(), 20),
            StringUtils.leftPad(String.valueOf(l), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(addTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(addiTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(negTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(negiTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(subTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(subiTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(muliTime), 10)
        );
    }
}
