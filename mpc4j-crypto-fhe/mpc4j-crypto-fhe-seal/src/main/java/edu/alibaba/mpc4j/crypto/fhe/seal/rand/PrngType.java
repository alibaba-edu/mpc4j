package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

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
    SHAKE256(2);

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
        return switch (value) {
            case 0 -> UNKNOWN;
            case 1 -> BLAKE2XB;
            case 2 -> SHAKE256;
            default -> throw new IllegalArgumentException("no match PRNG for given value");
        };
    }
}
