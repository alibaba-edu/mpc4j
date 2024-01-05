package edu.alibaba.mpc4j.crypto.fhe.rand;

/**
 * Clipped normal distribution.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/clipnormal.h#L86
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/20
 */
public class ClippedNormalDistribution {
    /**
     * mean μ
     */
    private final double mean;
    /**
     * standard deviation σ
     */
    private final double standardDeviation;
    /**
     * max deviation for clipping
     */
    private final double maxDeviation;

    public ClippedNormalDistribution(double mean, double standardDeviation, double maxDeviation) {
        if (standardDeviation < 0) {
            throw new IllegalArgumentException("standardDeviation must be >= 0");
        }
        if (maxDeviation < 0) {
            throw new IllegalArgumentException("maxDeviation must be >= 0");
        }
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.maxDeviation = maxDeviation;
    }

    /**
     * Samples a randomness in clipped normal distribution.
     *
     * @param engine uniform random generator engine.
     * @return a randomness in clipped normal distribution.
     */
    public double sample(UniformRandomGenerator engine) {
        while (true) {
            double value = engine.nextGaussian() * standardDeviation + mean;
            double deviation = Math.abs(value - mean);
            if (deviation <= maxDeviation) {
                return value;
            }
        }
    }

    /**
     * Returns mean μ.
     *
     * @return mean μ.
     */
    public double getMean() {
        return mean;
    }

    /**
     * Returns standard deviation σ.
     *
     * @return standard deviation σ.
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * Returns max deviation for clipping.
     *
     * @return max deviation for clipping.
     */
    public double getMaxDeviation() {
        return maxDeviation;
    }
}
