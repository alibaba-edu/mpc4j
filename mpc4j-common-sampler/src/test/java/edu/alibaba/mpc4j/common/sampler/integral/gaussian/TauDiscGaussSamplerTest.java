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

/**
 * Tests for Discrete Gaussian sampler with a cut-off parameter τ.
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
@RunWith(Parameterized.class)
public class TauDiscGaussSamplerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // ALIAS
        configurations.add(new Object[]{
            DiscGaussSamplerType.ALIAS.name(), DiscGaussSamplerType.ALIAS,
        });
        // SIGMA2_LOG_TABLE_TAU
        configurations.add(new Object[]{
            DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU.name(), DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU,
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

        return configurations;
    }

    /**
     * the discrete gaussian sampler type
     */
    private final DiscGaussSamplerType type;

    public TauDiscGaussSamplerTest(String name, DiscGaussSamplerType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testRatios() {
        // test proportional probabilities
        testRatios(3.0, 6);
        testRatios(2.0, 6);
        testRatios(4.0, 3);
        testRatios(15.4, 3);
    }

    private void testRatios(double sigma, int tau) {
        TauDiscGaussSampler sampler = DiscGaussSamplerFactory.createTauInstance(type, 0, sigma, tau);
        DiscGaussSamplerTestUtils.testRatios(sampler);
    }

    @Test
    public void testUniformBoundaries() {
        // test [⌊c⌋ - ⌈στ⌉, ..., ⌊c⌋ + ⌈στ⌉] boundaries
        testUniformBoundaries(0, 3.0, 2);
        testUniformBoundaries(0, 10.0, 2);
        testUniformBoundaries(1, 3.3, 1);
        testUniformBoundaries(2, 2.0, 2);
    }

    private void testUniformBoundaries(int c, double sigma, int tau) {
        TauDiscGaussSampler sampler = DiscGaussSamplerFactory.createTauInstance(type, c, sigma, tau);
        // [c - τ * σ, c + τ * σ]
        int lowerBound = (sampler.getC()) - (int) Math.ceil(sampler.getActualSigma() * sampler.getTau());
        int upperBound = (sampler.getC()) + (int) Math.ceil(sampler.getActualSigma() * sampler.getTau());

        for (int i = 0; i < DiscGaussSamplerTestUtils.N_TRIALS; i++) {
            int r = sampler.sample();
            if (r < lowerBound || r > upperBound) {
                System.out.println();
            }
            Assert.assertTrue(r >= lowerBound && r <= upperBound);
        }
    }

    @Test
    public void testMean() {
        testMean(0, 3.0, 6);
        testMean(0, 10.0, 6);
        testMean(1, 3.3, 6);
        testMean(2, 2.0, 6);

        testMean(0, 3.0, 3);
        testMean(0, 10.0, 3);
        testMean(1, 3.3, 3);
        testMean(2, 2.0, 3);
    }

    private void testMean(int c, double sigma, int tau) {
        TauDiscGaussSampler sampler = DiscGaussSamplerFactory.createTauInstance(type, c, sigma, tau);
        DiscGaussSamplerTestUtils.testMean(sampler);
    }
}
