package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.DiscGaussSamplerType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Discrete Gaussian sampler tests.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@RunWith(Parameterized.class)
public class DiscGaussSamplerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CONVOLUTION
        configurations.add(new Object[]{
            DiscGaussSamplerType.CONVOLUTION.name(), DiscGaussSamplerType.CONVOLUTION,
        });
        // ALIAS
        configurations.add(new Object[]{
            DiscGaussSamplerType.ALIAS.name(), DiscGaussSamplerType.ALIAS,
        });
        // SIGMA2_LOG_TABLE_TAU
        configurations.add(new Object[]{
            DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU.name(), DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU,
        });
        // SIGMA2_LOG_TABLE
        configurations.add(new Object[]{
            DiscGaussSamplerType.SIGMA2_LOG_TABLE.name(), DiscGaussSamplerType.SIGMA2_LOG_TABLE,
        });
        // UNIFORM_LOG_TABLE
        configurations.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_LOG_TABLE.name(), DiscGaussSamplerType.UNIFORM_LOG_TABLE,
        });
        // UNIFORM_ONLINE
        configurations.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_ONLINE.name(), DiscGaussSamplerType.UNIFORM_ONLINE,
        });
        // UNIFORM_TABLE
        configurations.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_TABLE.name(), DiscGaussSamplerType.UNIFORM_TABLE,
        });
        // CKS20_TAU
        configurations.add(new Object[]{
            DiscGaussSamplerType.CKS20_TAU.name(), DiscGaussSamplerType.CKS20_TAU,
        });
        // CKS20
        configurations.add(new Object[]{
            DiscGaussSamplerType.CKS20.name(), DiscGaussSamplerType.CKS20,
        });

        return configurations;
    }

    /**
     * the discrete gaussian sampler type
     */
    private final DiscGaussSamplerType type;

    public DiscGaussSamplerTest(String name, DiscGaussSamplerType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, 0, 1);
        Assert.assertEquals(type, sampler.getType());
    }

    @Test
    public void testSample() {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, 0, 1);
        int[] round1Samples = IntStream.range(0, DiscGaussSamplerTestUtils.N_TRIALS)
            .map(index -> sampler.sample())
            .toArray();
        int[] round2Samples = IntStream.range(0, DiscGaussSamplerTestUtils.N_TRIALS)
            .map(index -> sampler.sample())
            .toArray();
        // different sample results
        boolean allEqual = true;
        for (int i = 0; i < DiscGaussSamplerTestUtils.N_TRIALS; i++) {
            if (round1Samples[i] != round2Samples[i]) {
                allEqual = false;
                break;
            }
        }
        Assert.assertFalse(allEqual);
    }

    @Test
    public void testMean() {
        testMean(0, 3.0);
        testMean(0, 10.0);
        testMean(1, 3.3);
        testMean(2, 2.0);
    }

    private void testMean(int c, double sigma) {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, c, sigma);
        DiscGaussSamplerTestUtils.testMean(sampler);
    }

    @Test
    public void testRatios() {
        // test proportional probabilities
        testRatios(3.0);
        testRatios(2.0);
        testRatios(4.0);
        testRatios(15.4);
    }

    private void testRatios(double sigma) {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, 0, sigma);
        DiscGaussSamplerTestUtils.testRatios(sampler);
    }

    @Test
    public void testReseed() {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, new Random(), 0, 1);
        try {
            sampler.reseed(0L);
            int[] round1Samples = IntStream.range(0, DiscGaussSamplerTestUtils.N_TRIALS)
                .map(index -> sampler.sample())
                .toArray();
            sampler.reseed(0L);
            int[] round2Samples = IntStream.range(0, DiscGaussSamplerTestUtils.N_TRIALS)
                .map(index -> sampler.sample())
                .toArray();
            Assert.assertArrayEquals(round1Samples, round2Samples);
        } catch (UnsupportedOperationException ignored) {

        }
    }
}
