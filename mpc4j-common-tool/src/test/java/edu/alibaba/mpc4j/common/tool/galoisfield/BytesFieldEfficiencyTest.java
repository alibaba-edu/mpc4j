package edu.alibaba.mpc4j.common.tool.galoisfield;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
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
 * BytesField efficiency tests.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
@Ignore
public class BytesFieldEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BytesFieldEfficiencyTest.class);
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1 << 14;
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

    public BytesFieldEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testGf2kEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}",
            "                          type", "   mul(us)", "   div(us)", "   inv(us)"
        );
        for (Gf2kType type : Gf2kType.values()) {
            Gf2k gf2k = Gf2kFactory.createInstance(EnvType.STANDARD, type);
            testEfficiency(gf2k);
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testGf2eEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}",
            "                          type", "   mul(us)", "   div(us)", "   inv(us)"
        );
        for (int l : GaloisfieldTestUtils.GF2E_L_ARRAY) {
            for (Gf2eType type : Gf2eType.values()) {
                if (Gf2eFactory.available(type, l)) {
                    Gf2e gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, type, l);
                    testEfficiency(gf2e);
                }
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(BytesField bytesField) {
        // create random elements
        byte[][] arrayA = new byte[RANDOM_ROUND][];
        byte[][] arrayB = new byte[RANDOM_ROUND][];
        IntStream.range(0, RANDOM_ROUND).forEach(index -> {
            arrayA[index] = bytesField.createNonZeroRandom(secureRandom);
            arrayB[index] = bytesField.createNonZeroRandom(secureRandom);
        });
        // warmup
        IntStream.range(0, RANDOM_ROUND).forEach(index -> {
            bytesField.mul(arrayA[index], arrayB[index]);
            bytesField.div(arrayA[index], arrayB[index]);
            bytesField.inv(arrayA[index]);
        });
        // mul
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> bytesField.mul(arrayA[index], arrayB[index]));
        stopWatch.stop();
        double mulTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // div
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> bytesField.div(arrayA[index], arrayB[index]));
        stopWatch.stop();
        double divTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // inv
        stopWatch.start();
        IntStream.range(0, RANDOM_ROUND).forEach(index -> bytesField.inv(arrayA[index]));
        stopWatch.stop();
        double invTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / RANDOM_ROUND;
        stopWatch.reset();
        // output
        LOGGER.info(
            "{}\t{}\t{}\t{}",
            StringUtils.leftPad(bytesField.toString(), 30),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(divTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(invTime), 10)
        );
    }
}
