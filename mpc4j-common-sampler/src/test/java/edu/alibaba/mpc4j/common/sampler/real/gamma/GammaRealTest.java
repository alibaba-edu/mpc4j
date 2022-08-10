package edu.alibaba.mpc4j.common.sampler.real.gamma;

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
 * Gamma分布采样器测试。
 *
 * @author Weiran Liu
 * @date 2022/03/25
 */
@RunWith(Parameterized.class)
public class GammaRealTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GammaRealTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // ApacheSampler, k = 1.0, θ = 2.0
        GammaSampler apacheSampler01 = new ApacheGammaSampler(1.0, 2.0);
        configurationParams.add(new Object[] { apacheSampler01.toString(), apacheSampler01, });
        // ApacheSampler, k = 2.0, θ = 2.0
        GammaSampler apacheSampler02 = new ApacheGammaSampler(2.0, 2.0);
        configurationParams.add(new Object[] { apacheSampler02.toString(), apacheSampler02, });
        // ApacheSampler, k = 3.0, θ = 2.0
        GammaSampler apacheSampler03 = new ApacheGammaSampler(3.0, 2.0);
        configurationParams.add(new Object[] { apacheSampler03.toString(), apacheSampler03, });
        // ApacheSampler, k = 5.0, θ = 1.0
        GammaSampler apacheSampler04 = new ApacheGammaSampler(5.0, 1.0);
        configurationParams.add(new Object[] { apacheSampler04.toString(), apacheSampler04, });
        // ApacheSampler, k = 9.0, θ = 0.5
        GammaSampler apacheSampler05 = new ApacheGammaSampler(9.0, 0.5);
        configurationParams.add(new Object[] { apacheSampler05.toString(), apacheSampler05, });

        return configurationParams;
    }

    /**
     * 待测试的整数采样器
     */
    private final GammaSampler sampler;

    public GammaRealTest(String name, GammaSampler sampler) {
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
            if (!Precision.equals(round1Samples[i],round2Samples[i], DoubleUtils.PRECISION)) {
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
        Assert.assertEquals(sampler.getMean(), mean, 1.0);
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
