package edu.alibaba.mpc4j.common.sampler.integral.nb;

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
 * 负二项分布采样器测试。
 *
 * @author Weiran Liu
 * @date 2022/03/29
 */
@RunWith(Parameterized.class)
public class NbSamplerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NbSamplerTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // ApacheSampler, r = 1.0, p = 10.0 / 11.0，均值为10
        NbSampler apacheSampler01 = new ApacheNbSampler(1.0, 10.0 / (10.0 + 1.0));
        configurationParams.add(new Object[] { apacheSampler01.toString(), apacheSampler01, });
        // ApacheSampler, r = 2.0, p = 10.0 / 12.0，均值为10
        NbSampler apacheSampler02 = new ApacheNbSampler(2.0, 10.0 / (10.0 + 2.0));
        configurationParams.add(new Object[] { apacheSampler02.toString(), apacheSampler02, });
        // ApacheSampler, r = 3.0, p = 10.0 / 13.0，均值为10
        NbSampler apacheSampler03 = new ApacheNbSampler(3.0, 10.0 / (10.0 + 3.0));
        configurationParams.add(new Object[] { apacheSampler03.toString(), apacheSampler03, });
        // ApacheSampler, r = 4.0, p = 10.0 / 14.0，均值为10
        NbSampler apacheSampler04 = new ApacheNbSampler(4.0, 10.0 / (10.0 + 4.0));
        configurationParams.add(new Object[] { apacheSampler04.toString(), apacheSampler04, });
        // ApacheSampler, r = 5.0, p = 10.0 / 15.0，均值为10
        NbSampler apacheSampler05 = new ApacheNbSampler(5.0, 10.0 / (10.0 + 5.0));
        configurationParams.add(new Object[] { apacheSampler05.toString(), apacheSampler05, });
        // ApacheSampler, r = 10.0, p = 10.0 / 20.0，均值为10
        NbSampler apacheSampler06 = new ApacheNbSampler(10.0, 10.0 / (10.0 + 10.0));
        configurationParams.add(new Object[] { apacheSampler06.toString(), apacheSampler06, });
        // ApacheSampler, r = 20.0, p = 10.0 / 30.0，均值为10
        NbSampler apacheSampler07 = new ApacheNbSampler(20.0, 10.0 / (10.0 + 20.0));
        configurationParams.add(new Object[] { apacheSampler07.toString(), apacheSampler07, });
        // ApacheSampler, r = 40.0, p = 10.0 / 50.0，均值为10
        NbSampler apacheSampler08 = new ApacheNbSampler(40.0, 10.0 / (10.0 + 40.0));
        configurationParams.add(new Object[] { apacheSampler08.toString(), apacheSampler08, });

        return configurationParams;
    }

    /**
     * 待测试的整数采样器
     */
    private final NbSampler sampler;

    public NbSamplerTest(String name, NbSampler sampler) {
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
