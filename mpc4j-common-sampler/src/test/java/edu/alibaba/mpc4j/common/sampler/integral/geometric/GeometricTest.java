package edu.alibaba.mpc4j.common.sampler.integral.geometric;

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
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 整数Laplace分布采样器测试。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
@RunWith(Parameterized.class)
public class GeometricTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeometricTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // DiscreteGeometricSampler, μ = 0, b = 0.5
        GeometricSampler discreteSampler01 = new DiscreteGeometricSampler(new Random(), 0, 1, 2);
        configurationParams.add(new Object[] { discreteSampler01.toString(), discreteSampler01, });
        // JdkSampler, μ = 0, b = 0.5
        GeometricSampler jdkSampler01 = new JdkGeometricSampler(0, 0.5);
        configurationParams.add(new Object[] { jdkSampler01.toString(), jdkSampler01, });
        // ApacheSampler, μ = 0, b = 0.5
        GeometricSampler apacheSampler01 = new ApacheGeometricSampler(0, 0.5);
        configurationParams.add(new Object[] { apacheSampler01.toString(), apacheSampler01, });

        // DiscreteGeometricSampler, μ = 0, b = 1
        GeometricSampler discreteSampler02 = new DiscreteGeometricSampler(new Random(), 0, 2, 2);
        configurationParams.add(new Object[] { discreteSampler02.toString(), discreteSampler02, });
        // JdkSampler, μ = 0, b = 1.0
        GeometricSampler jdkSampler02 = new JdkGeometricSampler(0, 1.0);
        configurationParams.add(new Object[] { jdkSampler02.toString(), jdkSampler02, });
        // ApacheSampler, μ = 0, b = 1.0
        GeometricSampler apacheSampler02 = new ApacheGeometricSampler(0, 1.0);
        configurationParams.add(new Object[] { apacheSampler02.toString(), apacheSampler02, });

        // DiscreteGeometricSampler, μ = 0, b = 4.0
        GeometricSampler discreteSampler03 = new DiscreteGeometricSampler(new Random(), 0, 4, 1);
        configurationParams.add(new Object[] { discreteSampler03.toString(), discreteSampler03, });
        // JdkSampler, μ = 0, b = 4.0
        GeometricSampler jdkSampler03 = new JdkGeometricSampler(0, 4.0);
        configurationParams.add(new Object[] { jdkSampler03.toString(), jdkSampler03, });
        // ApacheSampler, μ = 0, b = 4.0
        GeometricSampler apacheSampler03 = new ApacheGeometricSampler(0, 4.0);
        configurationParams.add(new Object[] { apacheSampler03.toString(), apacheSampler03, });

        // DiscreteGeometricSampler, μ = -5, b = 1.0
        GeometricSampler discreteSampler04 = new DiscreteGeometricSampler(new Random(), -5, 1, 1);
        configurationParams.add(new Object[] { discreteSampler04.toString(), discreteSampler04, });
        // JdkSampler, μ = -5, b = 1.0
        GeometricSampler jdkSampler04 = new JdkGeometricSampler(-5, 1.0);
        configurationParams.add(new Object[] { jdkSampler04.toString(), jdkSampler04, });
        // ApacheSampler, μ = -5, b = 1.0
        GeometricSampler apacheSampler04 = new ApacheGeometricSampler(-5, 1.0);
        configurationParams.add(new Object[] { apacheSampler04.toString(), apacheSampler04, });

        return configurationParams;
    }

    /**
     * 待测试的整数采样器
     */
    private final GeometricSampler sampler;

    public GeometricTest(String name, GeometricSampler sampler) {
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
        // IntLaplace无法计算方差
        LOGGER.info("-----test params: {}-----", sampler);
        LOGGER.info("expect mean = {}, actual mean = {}", sampler.getMean(), mean);
        LOGGER.info("vars = {}", variance);
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
