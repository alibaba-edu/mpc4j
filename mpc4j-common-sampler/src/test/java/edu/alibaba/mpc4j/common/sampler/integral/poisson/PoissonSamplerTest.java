package edu.alibaba.mpc4j.common.sampler.integral.poisson;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
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
 * 泊松分布采样器测试。
 *
 * @author Weiran Liu
 * @date 2022/03/29
 */
@RunWith(Parameterized.class)
public class PoissonSamplerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PoissonSamplerTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // ApacheSampler, λ = 1.0
        PoissonSampler apacheSampler01 = new ApachePoissonSampler(1.0);
        configurationParams.add(new Object[] { apacheSampler01.toString(), apacheSampler01, });
        // ApacheSampler, k = 2.0, θ = 2.0
        PoissonSampler apacheSampler02 = new ApachePoissonSampler(4.0);
        configurationParams.add(new Object[] { apacheSampler02.toString(), apacheSampler02, });
        // ApacheSampler, k = 3.0, θ = 2.0
        PoissonSampler apacheSampler03 = new ApachePoissonSampler(10.0);
        configurationParams.add(new Object[] { apacheSampler03.toString(), apacheSampler03, });
        // ApacheSampler, λ = 0.1
        PoissonSampler apacheSampler04 = new ApachePoissonSampler(0.1);
        configurationParams.add(new Object[] { apacheSampler04.toString(), apacheSampler04, });

        return configurationParams;
    }

    /**
     * 待测试的整数采样器
     */
    private final PoissonSampler sampler;

    public PoissonSamplerTest(String name, PoissonSampler sampler) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.sampler = sampler;
    }

    @Test
    public void testSample() {
        int[] round1Samples = IntStream.range(0, SAMPLE_NUM)
            .map(index -> sampler.sample())
            .toArray();
        int[] round2Samples = IntStream.range(0, SAMPLE_NUM)
            .map(index -> sampler.sample())
            .toArray();
        // 两次采样结果应都不相同
        boolean allEqual = true;
        for (int i = 0; i < SAMPLE_NUM; i++) {
            if (round1Samples[i] != round2Samples[i]) {
                allEqual = false;
                break;
            }
        }
        Assert.assertFalse(allEqual);
    }

    @Test
    public void testParams() {
        int[] samples = IntStream.range(0, SAMPLE_NUM)
            .map(index -> sampler.sample())
            .toArray();
        double mean = Arrays.stream(samples).average().orElse(0);
        Assert.assertEquals(sampler.getMean(), mean, 1.0);
        double variance = Arrays.stream(samples)
            .mapToDouble(sample -> Math.pow(sample - mean, 2))
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
            int[] round1Samples = IntStream.range(0, SAMPLE_NUM)
                .map(index -> sampler.sample())
                .toArray();
            sampler.reseed(0L);
            int[] round2Samples = IntStream.range(0, SAMPLE_NUM)
                .map(index -> sampler.sample())
                .toArray();
            Assert.assertArrayEquals(round1Samples, round2Samples);
        } catch (UnsupportedOperationException ignored) {

        }
    }
}
