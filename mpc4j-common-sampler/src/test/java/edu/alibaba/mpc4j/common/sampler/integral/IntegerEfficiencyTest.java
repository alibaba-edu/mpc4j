package edu.alibaba.mpc4j.common.sampler.integral;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.ApacheGeometricSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.DiscreteGeometricSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.JdkGeometricSampler;
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
 * 整数采样器性能测试。
 *
 * @author Weiran Liu
 * @date 2022/03/29
 */
@Ignore
public class IntegerEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegerEfficiencyTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.000");
    /**
     * 计时器
     */
    private final StopWatch stopWatch;

    public IntegerEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testGeometricEfficiency() {
        LOGGER.info("{}\t{}", "      name", " time (us)");
        // μ = 0, b = 1.0
        testEfficiency("Apache", new ApacheGeometricSampler(0, 1.0));
        testEfficiency("JDK", new JdkGeometricSampler(0, 1.0));
        testEfficiency("Secure", new DiscreteGeometricSampler(0, 1, 1));
        // μ = 0, b = 2.0
        testEfficiency("Apache", new ApacheGeometricSampler(0, 2.0));
        testEfficiency("JDK", new JdkGeometricSampler(0, 2.0));
        testEfficiency("Secure", new DiscreteGeometricSampler(0, 2, 1));
        // μ = 0, b = 4.0
        testEfficiency("Apache", new ApacheGeometricSampler(0, 4.0));
        testEfficiency("JDK", new JdkGeometricSampler(0, 4.0));
        testEfficiency("Secure", new DiscreteGeometricSampler(0, 4, 1));
    }

    private void testEfficiency(String name, IntegralSampler sampler) {
        stopWatch.start();
        IntStream.range(0, SAMPLE_NUM).forEach(index -> sampler.sample());
        stopWatch.stop();
        double time = (double)stopWatch.getTime(TimeUnit.MICROSECONDS) / SAMPLE_NUM;
        stopWatch.reset();
        LOGGER.info("{}\t{}",
            StringUtils.leftPad(name, 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
        );
    }
}
