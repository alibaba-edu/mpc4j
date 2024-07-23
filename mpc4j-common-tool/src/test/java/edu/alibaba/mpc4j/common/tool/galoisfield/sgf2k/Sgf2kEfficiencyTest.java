package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
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
 * Subfield GF2K efficiency test.
 *
 * @author Weiran Liu
 * @date 2024/6/3
 */
@Ignore
public class Sgf2kEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sgf2kEfficiencyTest.class);
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1 << 10;
    /**
     * time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public Sgf2kEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}", "                          type",
            "mixMul(us)", "   mul(us)", "   div(us)", "   inv(us)"
        );
        Sgf2k001 sgf2k001 = new Sgf2k001(EnvType.STANDARD);
        testEfficiency(sgf2k001);
        for (int subfieldL : new int[]{2, 4, 8, 16, 32, 64}) {
            // NTL vs. Rings
            NtlSubSgf2k ntlSgf2k = new NtlSubSgf2k(EnvType.STANDARD, subfieldL);
            testEfficiency(ntlSgf2k);
            RingsSubSgf2k ringsSgf2k = new RingsSubSgf2k(EnvType.STANDARD, subfieldL);
            testEfficiency(ringsSgf2k);
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
        Sgf2k128 sgf2k128 = new Sgf2k128(EnvType.STANDARD);
        testEfficiency(sgf2k128);
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    private void testEfficiency(Sgf2k sgf2k) {
        // create random elements
        byte[][] ss = new byte[RANDOM_ROUND][];
        byte[][] ps = new byte[RANDOM_ROUND][];
        byte[][] qs = new byte[RANDOM_ROUND][];
        Gf2e subfield = sgf2k.getSubfield();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> {
            ss[index] = subfield.createRandom(secureRandom);
            ps[index] = sgf2k.createRandom(secureRandom);
            qs[index] = sgf2k.createRandom(secureRandom);
        });
        // warmup
        IntStream.range(0, RANDOM_ROUND).forEach(index -> {
            sgf2k.mixMul(ss[index], ps[index]);
            sgf2k.mul(ps[index], qs[index]);
            sgf2k.div(ps[index], qs[index]);
            sgf2k.inv(ps[index]);
        });
        // mix mul
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> sgf2k.mixMul(ss[index], ps[index]));
        stopWatch.stop();
        double mixMulTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // mul
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> sgf2k.mul(ps[index], qs[index]));
        stopWatch.stop();
        double mulTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // div
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> sgf2k.div(ps[index], qs[index]));
        stopWatch.stop();
        double divTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // inv
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> sgf2k.inv(ps[index]));
        stopWatch.stop();
        double invTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // output
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(sgf2k.toString(), 30),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mixMulTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(divTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(invTime), 10)
        );
    }
}
