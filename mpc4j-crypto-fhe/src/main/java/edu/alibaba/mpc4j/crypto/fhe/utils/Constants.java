package edu.alibaba.mpc4j.crypto.fhe.utils;

/**
 * This class provides global static constants.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/defines.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/3
 */
public class Constants {
    /**
     * private constructor.
     */
    private Constants() {
        // empty
    }

    /**
     * number of bytes per uint64
     */
    public static final int BYTES_PER_UINT64 = 8;
    /**
     * number of bits per uint64
     */
    public static final int UINT64_BITS = 64;

    /**
     * max bit-length of all coefficient moduli
     */
    public static final int SEAL_MOD_BIT_COUNT_MAX = 61;
    /**
     * min bit-length of all coefficient moduli
     */
    public static final int SEAL_MOD_BIT_COUNT_MIN = 2;

    /**
     * bit-length of internally used coefficient moduli, e.g., auxiliary base in BFV
     */
    public static final int SEAL_INTERNAL_MOD_BIT_COUNT = 61;

    /**
     * max bit-length of user-defined coefficient moduli
     */
    public static final int SEAL_USER_MOD_BIT_COUNT_MAX = 60;
    /**
     * min bit-length of user-defined coefficient moduli
     */
    public static final int SEAL_USER_MOD_BIT_COUNT_MIN = 2;

    /**
     * max bit-length of the plaintext modulus
     */
    public static final int SEAL_PLAIN_MOD_BIT_COUNT_MAX = SEAL_USER_MOD_BIT_COUNT_MAX;
    /**
     * min bit-length of the plaintext modulus
     */
    public static final int SEAL_PLAIN_MOD_BIT_COUNT_MIN = SEAL_USER_MOD_BIT_COUNT_MIN;

    /**
     * min number of coefficient moduli
     */
    public static final int SEAL_COEFF_MOD_COUNT_MIN = 1;
    /**
     * max number of coefficient moduli (no hard requirement)
     */
    public static final int SEAL_COEFF_MOD_COUNT_MAX = 64;

    /**
     * max polynomial modulus degree (no hard requirement)
     */
    public static final int SEAL_POLY_MOD_DEGREE_MAX = 131072;
    /**
     * min polynomial modulus degree
     */
    public static final int SEAL_POLY_MOD_DEGREE_MIN = 2;

    /**
     * max size of a ciphertext (cannot exceed 2^32 / poly_modulus_degree)
     */
    public static final int SEAL_CIPHERTEXT_SIZE_MAX = 16;
    /**
     * min size of a ciphertext
     */
    public static final int SEAL_CIPHERTEXT_SIZE_MIN = 2;

    /**
     * How many pairs of modular integers can we multiply and accumulate in a 128-bit data type
     */
    public static final int MULTIPLY_ACCUMULATE_MOD_MAX = (1 << (128 - (SEAL_MOD_BIT_COUNT_MAX << 1)));
    /**
     * How many pairs of modular integers can user multiply and accumulate in a 128-bit data type
     */
    public static final int MULTIPLY_ACCUMULATE_USER_MOD_MAX = (1 << (128 - (SEAL_USER_MOD_BIT_COUNT_MAX << 1)));
}
