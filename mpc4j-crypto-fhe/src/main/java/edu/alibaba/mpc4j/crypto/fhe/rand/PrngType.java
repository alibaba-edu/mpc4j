package edu.alibaba.mpc4j.crypto.fhe.rand;

/**
 * A type indicating a specific pseud-random number generator.
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public enum PrngType {
    /**
     * unknown
     */
    UNKNOWN(0),
    /**
     * blake2xb
     */
    BLAKE2XB(1),
    /**
     * shake256
     */
    SHAKE256(2),
    /**
     * SHA1PRNG
     */
    SHA1PRNG(3);

    /**
     * the index of the prng_type
     */
    private final int value;

    /**
     * Creates an prng_type.
     *
     * @param value the index of the prng_type.
     */
    PrngType(int value) {
        this.value = value;
    }

    /**
     * Gets the index of the prng_type.
     *
     * @return the index of the prng_type.
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets SchemeType by the index.
     *
     * @param value the index of the SchemeType.
     * @return the corresponding SchemeType.
     */
    public static PrngType getByValue(int value) {
        switch (value) {
            case 0:
                return UNKNOWN;
            case 1:
                return BLAKE2XB;
            case 2:
                return SHAKE256;
            case 3:
                return SHA1PRNG;
            default:
                throw new IllegalArgumentException("no match PRNG for given value");
        }
    }
}
