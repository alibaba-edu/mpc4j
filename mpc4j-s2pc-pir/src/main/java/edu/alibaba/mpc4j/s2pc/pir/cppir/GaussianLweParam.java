package edu.alibaba.mpc4j.s2pc.pir.cppir;

/**
 * LWE parameters with Gaussian Noise.
 *
 * @author Weiran Liu
 * @date 2024/9/2
 */
public enum GaussianLweParam {
    /**
     * dimension = 1024, σ = 6.4, used in SimplePIR (See Section 4.2).
     */
    N_1024_SIGMA_6_4(1024, 6.4),
    /**
     * dimension = 1408, σ = 6.4, used in HintlessPIR (See Section 7.1).
     */
    N_1408_SIGMA_6_4(1408, 6.4);

    /**
     * dimension
     */
    private final int dimension;
    /**
     * error distribution: (0, σ) - discrete Gaussian distribution
     */
    private final double sigma;

    GaussianLweParam(int dimension, double sigma) {
        this.dimension = dimension;
        this.sigma = sigma;
    }

    /**
     * Gets dimension.
     *
     * @return dimension.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Gets σ.
     *
     * @return σ.
     */
    public double getSigma() {
        return sigma;
    }
}
