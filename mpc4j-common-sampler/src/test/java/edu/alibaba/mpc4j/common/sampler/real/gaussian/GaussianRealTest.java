package edu.alibaba.mpc4j.common.sampler.real.gaussian;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * 高斯采样器测试。
 *
 * @author Weiran Liu
 * @date 2022/03/25
 */
@RunWith(Parameterized.class)
public class GaussianRealTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GaussianRealTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // ApacheSampler, μ = 0, σ = 1
        GaussianSampler apacheSampler01 = new ApacheGaussianSampler(0.0, 1.0);
        configurationParams.add(new Object[] { apacheSampler01.toString(), apacheSampler01, });
        // ApacheSampler, μ = 0, σ = 2
        GaussianSampler apacheSampler02 = new ApacheGaussianSampler(0.0, 2.0);
        configurationParams.add(new Object[] { apacheSampler02.toString(), apacheSampler02, });
        // ApacheSampler, μ = 0, σ = 4
        GaussianSampler apacheSampler03 = new ApacheGaussianSampler(0.0, 4.0);
        configurationParams.add(new Object[] { apacheSampler03.toString(), apacheSampler03, });
        // ApacheSampler, μ = -5, σ = 4
        GaussianSampler apacheSampler04 = new ApacheGaussianSampler(-5.0, 4.0);
        configurationParams.add(new Object[] { apacheSampler04.toString(), apacheSampler04, });

        // GoogleSampler, μ = 0, σ = 1
        GaussianSampler googleSampler01 = new GoogleGaussianSampler(0.0, 1.0);
        configurationParams.add(new Object[] { googleSampler01.toString(), googleSampler01, });
        // GoogleSampler, μ = 0, σ = 2
        GaussianSampler googleSampler02 = new GoogleGaussianSampler(0.0, 2.0);
        configurationParams.add(new Object[] { googleSampler02.toString(), googleSampler02, });
        // GoogleSampler, μ = 0, σ = 4
        GaussianSampler googleSampler03 = new GoogleGaussianSampler(0.0, 4.0);
        configurationParams.add(new Object[] { googleSampler03.toString(), googleSampler03, });
        // GoogleSampler, μ = -5, σ = 4
        GaussianSampler googleSampler04 = new GoogleGaussianSampler(-5.0, 4.0);
        configurationParams.add(new Object[] { googleSampler04.toString(), googleSampler04, });

        return configurationParams;
    }

    /**
     * 待测试的整数采样器
     */
    private final GaussianSampler sampler;

    public GaussianRealTest(String name, GaussianSampler sampler) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.sampler = sampler;
    }

    @Test
    public void testSample() {
        double[] round1Samples = IntStream.range(0, SAMPLE_NUM)
            .mapToDouble(index -> sampler.sample())
            .toArray();
        double[] round2Samples = IntStream.range(0, SAMPLE_NUM)
            .mapToDouble(index -> sampler.sample())
            .toArray();
        // 两次采样结果应都不相同
        boolean allEqual = true;
        for (int i = 0; i < SAMPLE_NUM; i++) {
            if (!Precision.equals(round1Samples[i], round2Samples[i], DoubleUtils.PRECISION)) {
                allEqual = false;
                break;
            }
        }
        Assert.assertFalse(allEqual);
    }

    @Test
    public void testParams() {
        double[] samples = IntStream.range(0, SAMPLE_NUM)
            .mapToDouble(index -> sampler.sample())
            .toArray();
        double mean = Arrays.stream(samples).average().orElse(0);
        Assert.assertEquals(sampler.getMean(), mean, 0.1);
        double variance = Arrays.stream(samples)
            .map(sample -> Math.pow(sample - mean, 2))
            .sum() / SAMPLE_NUM;
        Assert.assertEquals(sampler.getVariance(), variance, sampler.getVariance() * 0.3);
        LOGGER.info("-----test params: {}-----", sampler);
        LOGGER.info("expect mean = {}, actual mean = {}", sampler.getMean(), mean);
        LOGGER.info("expect vars = {}, actual vars = {}", sampler.getVariance(), variance);
    }

    @Test
    public void testReseed() {
        try {
            sampler.reseed(0L);
            double[] round1Samples = IntStream.range(0, SAMPLE_NUM)
                .mapToDouble(index -> sampler.sample())
                .toArray();
            sampler.reseed(0L);
            double[] round2Samples = IntStream.range(0, SAMPLE_NUM)
                .mapToDouble(index -> sampler.sample())
                .toArray();
            Assert.assertArrayEquals(round1Samples, round2Samples, DoubleUtils.PRECISION);
        } catch (UnsupportedOperationException ignored) {

        }
    }
}
