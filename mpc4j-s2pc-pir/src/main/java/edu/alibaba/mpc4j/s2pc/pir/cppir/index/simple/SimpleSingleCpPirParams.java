package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;

/**
 * Simple client-specific preprocessing PIR scheme params with 128-bit security.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
class SimpleSingleCpPirParams {
    /**
     * the secret dimension, Section 4.2 of the paper requires n = 2^10
     */
    static final int N = 1 << 10;
    /**
     * ciphertext modulus, Section 4.2 of the paper requires q = 2^32
     */
    static final long Q = 1L << 32;
    /**
     * error distribution: (0, σ) - discrete Gaussian distribution, Section 4.2 of the paper requires σ = 6.4,
     */
    static final double SIGMA = 6.4;
    /**
     * plaintext modulus bit length, can be chosen in {991, 833, 701, 589}, all bit length are 10.
     */
    static final int LOG_P = 10;
    /**
     * plaintext modulus, can be chosen in {991, 833, 701, 589}
     */
    final int p;
    /**
     * zl64
     */
    final Zl64 zl64;

    /**
     * Creates SimplePIR params.
     *
     * @param envType environment.
     * @param logN    log(N), where N is the database size.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    public SimpleSingleCpPirParams(EnvType envType, int logN) {
        MathPreconditions.checkNonNegativeInRangeClosed("logN", logN, 16);
        if (logN <= 13) {
            // n = 2^13, p = 991
            p = 991;
        } else if (logN == 14) {
            p = 833;
        } else if (logN == 15) {
            p = 701;
        } else if (logN == 16) {
            p = 589;
        } else {
            throw new IllegalStateException("logN must be in range [0, " + 16 + "]: " + logN);
        }
        zl64 = Zl64Factory.createInstance(envType, 32);
    }
}
