package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;

/**
 * Discrete Gaussian Sampler test utilities.
 *
 * @author Weiran Liu
 * @date 2022/11/28
 */
class DiscGaussSamplerTestUtils {

    private DiscGaussSamplerTestUtils() {
        // empty
    }

    /**
     * number of trials
     */
    static final int N_TRIALS = 1 << 18;
    /**
     * tolerance
     */
    static final double TOLERANCE = 0.1;
    /**
     * bound
     */
    static final int BOUND = 2;

    static void testRatios(DiscGaussSampler sampler) {
        // counts number of samples in [-Bound, BOUND]
        double[] counts = new double[2 * DiscGaussSamplerTestUtils.BOUND + 1];
        for (int i = 0; i < DiscGaussSamplerTestUtils.N_TRIALS; i++) {
            int r = sampler.sample();
            if (Math.abs(r) <= DiscGaussSamplerTestUtils.BOUND) {
                counts[r + DiscGaussSamplerTestUtils.BOUND] += 1;
            }
        }
        // calculate ratios for each count pairs
        for (int i = -DiscGaussSamplerTestUtils.BOUND; i <= DiscGaussSamplerTestUtils.BOUND; i++) {
            // for very large σ, right-most bound may be 0. In this case, we allow left to be 1.
            double left;
            if (Precision.compareTo(counts[DiscGaussSamplerTestUtils.BOUND + i], 0, DoubleUtils.PRECISION) == 0) {
                Assert.assertEquals(counts[DiscGaussSamplerTestUtils.BOUND + 1], 0, DoubleUtils.PRECISION);
                left = 1;
            } else {
                left = counts[DiscGaussSamplerTestUtils.BOUND + 1] / counts[DiscGaussSamplerTestUtils.BOUND + i];
            }
            double right = rho(0, sampler.getActualSigma()) / rho(i, sampler.getActualSigma());
            Assert.assertTrue(Math.abs(Math.log(left / right)) < DiscGaussSamplerTestUtils.TOLERANCE * 4);
        }
    }

    private static double rho(double x, double sigma) {
        return Math.exp(-(x * x) / (2 * sigma * sigma));
    }

    static void testMean(DiscGaussSampler sampler) {
        double mean = 0.0;
        for (int i = 0; i < DiscGaussSamplerTestUtils.N_TRIALS; i++) {
            mean += sampler.sample();
        }
        mean /= DiscGaussSamplerTestUtils.N_TRIALS;
        Assert.assertTrue(Math.abs(mean - sampler.getC()) <= DiscGaussSamplerTestUtils.TOLERANCE);
    }
}
