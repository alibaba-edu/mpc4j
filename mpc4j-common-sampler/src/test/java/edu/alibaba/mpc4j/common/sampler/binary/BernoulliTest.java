package edu.alibaba.mpc4j.common.sampler.binary;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ApacheBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.BernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
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
 * 伯努利分布采样器测试。
 *
 * @author Weiran Liu
 * @date 2022/03/24
 */
@RunWith(Parameterized.class)
public class BernoulliTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BernoulliTest.class);
    /**
     * 采样数量
     */
    private static final int SAMPLE_NUM = 100000;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // ApacheSampler, p = 0
        BernoulliSampler apacheSampler01 = new ApacheBernoulliSampler(0.0);
        configurationParams.add(new Object[] { apacheSampler01.toString(), apacheSampler01, });
        // ApacheSampler, p = 1
        BernoulliSampler apacheSampler02 = new ApacheBernoulliSampler(1.0);
        configurationParams.add(new Object[] { apacheSampler02.toString(), apacheSampler02, });
        // ApacheSampler, p = 0.5
        BernoulliSampler apacheSampler03 = new ApacheBernoulliSampler(0.5);
        configurationParams.add(new Object[] { apacheSampler03.toString(), apacheSampler03, });

        // SecureSampler, p = 0
        BernoulliSampler secureSampler01 = new SecureBernoulliSampler(new Random(), 0.0);
        configurationParams.add(new Object[] { secureSampler01.toString(), secureSampler01, });
        // SecureSampler, p = 1
        BernoulliSampler secureSampler02 = new SecureBernoulliSampler(new Random(), 1.0);
        configurationParams.add(new Object[] { secureSampler02.toString(), secureSampler02, });
        // SecureSampler, p = 0.5
        BernoulliSampler secureSampler03 = new SecureBernoulliSampler(new Random(), 0.5);
        configurationParams.add(new Object[] { secureSampler03.toString(), secureSampler03, });

        // ExpSampler, γ = 0
        BernoulliSampler expSampler01 = new ExpBernoulliSampler(new Random(), 0.0);
        configurationParams.add(new Object[] { expSampler01.toString(), expSampler01, });
        // ExpSampler, γ = 1
        BernoulliSampler expSampler02 = new ExpBernoulliSampler(new Random(), 1.0);
        configurationParams.add(new Object[] { expSampler02.toString(), expSampler02, });
        // ExpSampler, γ = 2
        BernoulliSampler expSampler03 = new ExpBernoulliSampler(new Random(), 2.0);
        configurationParams.add(new Object[] { expSampler03.toString(), expSampler03, });

        return configurationParams;
    }

    /**
     * 待测试的整数采样器
     */
    private final BernoulliSampler sampler;

    public BernoulliTest(String name, BernoulliSampler sampler) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.sampler = sampler;
    }

    @Test
    public void testSample() {
        // p = 0或p = 1时采样结果一定相同
        if (sampler.getP() > 0 && sampler.getP() < 1) {
            int[] round1Samples = IntStream.range(0, SAMPLE_NUM)
                .map(index -> {
                    boolean sample = sampler.sample();
                    return sample ? 1 : 0;
                })
                .toArray();
            int[] round2Samples = IntStream.range(0, SAMPLE_NUM)
                .map(index -> {
                    boolean sample = sampler.sample();
                    return sample ? 1 : 0;
                })
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
    }

    @Test
    public void testParams() {
        int[] samples = IntStream.range(0, SAMPLE_NUM)
            .map(index -> {
                boolean sample = sampler.sample();
                return sample ? 1 : 0;
            })
            .toArray();
        double mean = Arrays.stream(samples).average().orElse(0);
        Assert.assertEquals(sampler.getMean(), mean, 0.1);
        double variance = Arrays.stream(samples)
            .mapToDouble(sample -> Math.pow(sample - mean, 2))
            .sum() / SAMPLE_NUM;
        Assert.assertEquals(sampler.getVariance(), variance, 0.1);
        LOGGER.info("-----test params: {}-----", sampler);
        LOGGER.info("expect mean = {}, actual mean = {}", sampler.getMean(), mean);
        LOGGER.info("expect vars = {}, actual vars = {}", sampler.getVariance(), variance);
    }

    @Test
    public void testReseed() {
        try {
            sampler.reseed(0L);
            int[] round1Samples = IntStream.range(0, SAMPLE_NUM)
                .map(index -> {
                    boolean sample = sampler.sample();
                    return sample ? 1 : 0;
                })
                .toArray();
            sampler.reseed(0L);
            int[] round2Samples = IntStream.range(0, SAMPLE_NUM)
                .map(index -> {
                    boolean sample = sampler.sample();
                    return sample ? 1 : 0;
                })
                .toArray();
            Assert.assertArrayEquals(round1Samples, round2Samples);
        } catch (UnsupportedOperationException ignored) {

        }
    }
}
