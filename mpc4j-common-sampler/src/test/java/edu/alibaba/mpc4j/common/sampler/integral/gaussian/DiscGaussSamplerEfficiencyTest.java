package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.DiscGaussSamplerType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Discrete Gaussian sampler efficiency tests.
 *
 * @author Weiran Liu
 * @date 2022/11/28
 */
@Ignore
public class DiscGaussSamplerEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscGaussSamplerEfficiencyTest.class);
    /**
     * time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * types
     */
    private static final DiscGaussSamplerType[] TYPES = {
        DiscGaussSamplerType.UNIFORM_ONLINE,
        DiscGaussSamplerType.UNIFORM_TABLE,
        DiscGaussSamplerType.UNIFORM_LOG_TABLE,
        DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU,
        DiscGaussSamplerType.SIGMA2_LOG_TABLE,
        DiscGaussSamplerType.ALIAS,
        DiscGaussSamplerType.CONVOLUTION,
        DiscGaussSamplerType.CKS20_TAU,
        DiscGaussSamplerType.CKS20,
    };
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public DiscGaussSamplerEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                name", "         σ", " time (us)");
        testEfficiency(1);
        testEfficiency(2);
        testEfficiency(4);
        testEfficiency(8);
        testEfficiency(16);
    }

    private void testEfficiency(double sigma) {
        for (DiscGaussSamplerType type : TYPES) {
            testEfficiency(type, sigma);
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    private void testEfficiency(DiscGaussSamplerType type, double sigma) {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, new Random(), 0, sigma);
        stopWatch.start();
        IntStream.range(0, DiscGaussSamplerTestUtils.N_TRIALS).forEach(index -> sampler.sample());
        stopWatch.stop();
        double sampleTime = (double)stopWatch.getTime(TimeUnit.MICROSECONDS) / DiscGaussSamplerTestUtils.N_TRIALS;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad(type.name(), 20),
            StringUtils.leftPad(String.valueOf(sigma), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(sampleTime), 10)
        );
    }
}
