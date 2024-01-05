package edu.alibaba.mpc4j.crypto.fhe.context;

/**
 * Identifies the reason why encryption parameters are not valid.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/context.h#L34
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/11
 */
public enum ErrorType {
    /**
     * constructed but not yet validated
     */
    NONE(-1),
    /**
     * valid
     */
    SUCCESS(0),
    /**
     * scheme must be BFV or CKKS or BGV
     */
    INVALID_SCHEME(1),
    /**
     * coeff_modulus's primes' count is not bounded by COEFF_MOD_COUNT_MIN(MAX)
     */
    INVALID_COEFF_MODULUS_SIZE(2),
    /**
     * coeff_modulus's primes' bit counts are not bounded by USER_MOD_BIT_COUNT_MIN(MAX)
     */
    INVALID_COEFF_MODULUS_BIT_COUNT(3),
    /**
     * coeff_modulus's primes are not congruent to 1 modulo (2 * poly_modulus_degree)
     */
    INVALID_COEFF_MODULUS_NO_NTT(4),
    /**
     * poly_modulus_degree is not bounded by POLY_MOD_DEGREE_MIN(MAX)
     */
    INVALID_POLY_MODULUS_DEGREE(5),
    /**
     * poly_modulus_degree is not a power of two
     */
    INVALID_POLY_MODULUS_DEGREE_NON_POWER_OF_TWO(6),
    /**
     * parameters are too large to fit in size_t type
     */
    INVALID_PARAMETERS_TOO_LARGE(7),
    /**
     * parameters are not compliant with HomomorphicEncryption.org security standard
     */
    INVALID_PARAMETERS_INSECURE(8),
    /**
     * RNSBase cannot be constructed
     */
    FAILED_CREATING_RNS_BASE(9),
    /**
     * plain_modulus's bit count is not bounded by SEAL_PLAIN_MOD_BIT_COUNT_MIN(MAX)
     */
    INVALID_PLAIN_MODULUS_BIT_COUNT(10),
    /**
     * plain_modulus is not co-prime to coeff_modulus
     */
    INVALID_PLAIN_MODULUS_CO_PRIMALITY(11),
    /**
     * plain_modulus is not smaller than coeff_modulus
     */
    INVALID_PLAIN_MODULUS_TOO_LARGE(12),
    /**
     * plain_modulus is not zero
     */
    INVALID_PLAIN_MODULUS_NONZERO(13),
    /**
     * RNSTool cannot be constructed
     */
    FAILED_CREATING_RNS_TOOL(14);

    /**
     * the index of the error_type.
     */
    private final int value;

    /**
     * Creates an error_type.
     *
     * @param value the index of the error_type.
     */
    ErrorType(int value) {
        this.value = value;
    }

    /**
     * Gets the index of the error_type.
     *
     * @return the index of the error_type.
     */
    public int getValue() {
        return value;
    }
}
