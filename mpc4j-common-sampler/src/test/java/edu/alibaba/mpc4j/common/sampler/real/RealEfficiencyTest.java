package edu.alibaba.mpc4j.common.sampler.real;

import edu.alibaba.mpc4j.common.sampler.real.gamma.ApacheGammaSampler;
import edu.alibaba.mpc4j.common.sampler.real.gaussian.ApacheGaussianSampler;
import edu.alibaba.mpc4j.common.sampler.real.gaussian.GoogleGaussianSampler;
import edu.alibaba.mpc4j.common.sampler.real.laplace.ApacheLaplaceSampler;
import edu.alibaba.mpc4j.common.sampler.real.laplace.GoogleLaplaceSampler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 实数采样器性能测试。
 *
 * @author Weiran Liu
 * @date 2022/03/29
 */
@Ignore
public class RealEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealEfficiencyTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;
    /**
     * 方差输出格式
     */
    private static final DecimalFormat VAR_DECIMAL_FORMAT = new DecimalFormat("00.0");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.000");
    /**
     * 计时器
     */
    private final StopWatch stopWatch;

    public RealEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testGaussianEfficiency() {
        LOGGER.info("{}\t{}\t{}", "      name", "      var.", " time (us)");
        // μ = 0, b = 1
        testEfficiency("Apache", new ApacheGaussianSampler(0.0, 1.0));
        testEfficiency("Google", new GoogleGaussianSampler(0.0, 1.0));
        // μ = 0, b = 2
        testEfficiency("Apache", new ApacheGaussianSampler(0.0, 2.0));
        testEfficiency("Secure", new GoogleGaussianSampler(0.0, 2.0));
        // μ = 0, b = 4
        testEfficiency("Apache", new ApacheGaussianSampler(0.0, 4.0));
        testEfficiency("Secure", new GoogleGaussianSampler(0.0, 4.0));
    }

    @Test
    public void testGammaEfficiency() {
        LOGGER.info("{}\t{}\t{}", "      name", "      var.", " time (us)");
        // k = 1.0, θ = 2.0
        testEfficiency("Apache", new ApacheGammaSampler(1.0, 2.0));
        // k = 2.0, θ = 2.0
        testEfficiency("Apache", new ApacheGammaSampler(2.0, 2.0));
        // k = 3.0, θ = 2.0
        testEfficiency("Apache", new ApacheGammaSampler(3.0, 2.0));
        // k = 5.0, θ = 1.0
        testEfficiency("Apache", new ApacheGammaSampler(5.0, 1.0));
        // k = 9.0, θ = 0.5
        testEfficiency("Apache", new ApacheGammaSampler(9.0, 0.5));
    }

    @Test
    public void testLaplaceEfficiency() {
        LOGGER.info("{}\t{}\t{}", "      name", "      var.", " time (us)");
        // μ = 0, b = 1
        testEfficiency("Apache", new ApacheLaplaceSampler(0.0, 1.0));
        testEfficiency("Google", new GoogleLaplaceSampler(0.0, 1.0));
        // μ = 0, b = 2
        testEfficiency("Apache", new ApacheLaplaceSampler(0.0, 2.0));
        testEfficiency("Secure", new GoogleLaplaceSampler(0.0, 2.0));
        // μ = 0, b = 4
        testEfficiency("Apache", new ApacheLaplaceSampler(0.0, 4.0));
        testEfficiency("Secure", new GoogleLaplaceSampler(0.0, 4.0));
    }

    private void testEfficiency(String name, RealSampler sampler) {
        stopWatch.start();
        IntStream.range(0, SAMPLE_NUM).forEach(index -> sampler.sample());
        stopWatch.stop();
        double time = (double)stopWatch.getTime(TimeUnit.MICROSECONDS) / SAMPLE_NUM;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad(name, 10),
            StringUtils.leftPad(VAR_DECIMAL_FORMAT.format(sampler.getVariance()), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
        );
    }
}
