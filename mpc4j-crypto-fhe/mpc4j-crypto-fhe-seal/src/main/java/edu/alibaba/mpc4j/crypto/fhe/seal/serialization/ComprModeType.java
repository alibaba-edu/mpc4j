package edu.alibaba.mpc4j.crypto.fhe.seal.serialization;

/**
 * A type to describe the compression algorithm applied to serialized data.
 * Ciphertext and key data consist of a large number of 64-bit words storing
 * integers modulo prime numbers much smaller than the word size, resulting in
 * a large number of zero bytes in the output. Any compression algorithm should
 * be able to clean up these zero bytes and hence compress both ciphertext and
 * key data.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/serialization.h#23">serialization.h</a>.
 *
 * @author Weiran Liu
 * @date 2023/12/11
 */
public enum ComprModeType {
    /**
     * No compression is used.
     */
    NONE(0),
    /**
     * Use ZLIB compression
     */
    ZLIB(1),
    /**
     * Use Zstandard compression
     */
    ZSTD(2);

    /**
     * the index of the SchemeType
     */
    private final int value;

    /**
     * Creates a SchemeType.
     *
     * @param value the index of the SchemeType.
     */
    ComprModeType(int value) {
        this.value = value;
    }

    /**
     * Gets the index of the SchemeType.
     *
     * @return the index of the SchemeType.
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
    public static ComprModeType getByValue(int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> ZLIB;
            case 2 -> ZSTD;
            default -> throw new IllegalArgumentException("no match compression mode for given value");
        };
    }
}
