package edu.alibaba.mpc4j.crypto.phe.params;

import edu.alibaba.mpc4j.crypto.phe.PheSecLevel;

/**
 * PHE key generation parameters.
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public class PheKeyGenParams {
    /**
     * default base, for which the original plaintext is encoded as <code>value * base<sup>exponent</sup></code>.
     */
    private static final int DEFAULT_BASE = 16;
    /**
     * PHE security level.
     */
    private final PheSecLevel pheSecLevel;
    /**
     * signed encoding or not
     */
    private final boolean signed;
    /**
     * encode precision
     */
    private final int precision;
    /**
     * base, for which the original plaintext is encoded as <code>value * base<sup>exponent</sup></code>.
     */
    private final int base;

    /**
     * Creates a PHE key generation parameters with default base.
     *
     * @param pheSecLevel PHE security level.
     * @param signed      signed encoding or not.
     * @param precision   precision.
     */
    public PheKeyGenParams(PheSecLevel pheSecLevel, boolean signed, int precision) {
        this(pheSecLevel, signed, precision, DEFAULT_BASE);
    }

    /**
     * Creates a PHE key generation parameters.
     *
     * @param pheSecLevel PHE security level.
     * @param signed      signed encoding or not.
     * @param precision   precision.
     * @param base        base.
     */
    public PheKeyGenParams(PheSecLevel pheSecLevel, boolean signed, int precision, int base) {
        this.pheSecLevel = pheSecLevel;
        this.signed = signed;
        this.precision = precision;
        this.base = base;
    }

    /**
     * Gets the PHE security level.
     *
     * @return PHE security level.
     */
    public PheSecLevel getPheSecLevel() {
        return pheSecLevel;
    }

    /**
     * Gets the signed encoding or not.
     *
     * @return signed encoding or not.
     */
    public boolean isSigned() {
        return signed;
    }

    /**
     * Gets the precision.
     *
     * @return precision.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Gets the base.
     *
     * @return base.
     */
    public int getBase() {
        return base;
    }
}
