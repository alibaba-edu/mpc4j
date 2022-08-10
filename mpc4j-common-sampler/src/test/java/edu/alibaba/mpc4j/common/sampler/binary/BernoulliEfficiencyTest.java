package edu.alibaba.mpc4j.common.sampler.binary;

import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ApacheBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.BernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 伯努利分布采样器性能测试。
 *
 * @author Weiran Liu
 * @date 2022/03/25
 */
@Ignore
public class BernoulliEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BernoulliEfficiencyTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;
    /**
     * p输出格式
     */
    private static final DecimalFormat P_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.000");
    /**
     * 计时器
     */
    private final StopWatch stopWatch;

    public BernoulliEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t\t{}\t{}", "name", "p", "time (us)");
        // p = e^{-1}
        testEfficiency("Apache", new ApacheBernoulliSampler(Math.exp(-1.0)));
        testEfficiency("Secure", new SecureBernoulliSampler(Math.exp(-1.0)));
        testEfficiency("Exp.  ", new ExpBernoulliSampler(1.0));
        // p = e^{-2}
        testEfficiency("Apache", new ApacheBernoulliSampler(Math.exp(-2.0)));
        testEfficiency("Secure", new SecureBernoulliSampler(Math.exp(-2.0)));
        testEfficiency("Exp.  ", new ExpBernoulliSampler(2.0));
    }

    private void testEfficiency(String name, BernoulliSampler sampler) {
        stopWatch.start();
        IntStream.range(0, SAMPLE_NUM).forEach(index -> sampler.sample());
        stopWatch.stop();
        double time = (double)stopWatch.getTime(TimeUnit.MICROSECONDS) / SAMPLE_NUM;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}", name, P_DECIMAL_FORMAT.format(sampler.getP()), TIME_DECIMAL_FORMAT.format(time));
    }
}
