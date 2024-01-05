package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import java.util.Map;

/**
 * doubly oblivious key-value storage with values in GF(2^κ).
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public interface Gf2kDokvs<T> {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    Gf2kDokvsFactory.Gf2kDokvsType getType();

    /**
     * Sets parallel encode.
     *
     * @param parallelEncode parallel encode.
     */
    void setParallelEncode(boolean parallelEncode);

    /**
     * Gets parallel encode.
     *
     * @return parallel encode.
     */
    boolean getParallelEncode();

    /**
     * Encodes the key-value map.
     *
     * @param keyValueMap  key-value map.
     * @param doublyEncode encode with doubly obliviousness.
     * @return encoded storage.
     * @throws ArithmeticException if we cannot finish encoding.
     */
    byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException;

    /**
     * Decodes the key.
     *
     * @param storage encoded storage.
     * @param key     key.
     * @return value.
     */
    byte[] decode(byte[][] storage, T key);

    /**
     * Gets the number of keys to encode.
     *
     * @return the number of keys to encode.
     */
    int getN();

    /**
     * Gets the size of the encoded storage. The size m must satisfy {@code m / Byte.SIZE == 0}.
     *
     * @return the size of the encoded storage.
     */
    int getM();

    /**
     * Gets the encode rate, i.e., n / m.
     *
     * @return the encode rate.
     */
    default double rate() {
        return ((double) this.getN()) / this.getM();
    }
}
