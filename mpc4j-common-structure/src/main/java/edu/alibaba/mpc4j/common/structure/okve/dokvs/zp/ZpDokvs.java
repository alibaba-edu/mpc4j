package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;

import java.math.BigInteger;
import java.util.Map;

/**
 * doubly oblivious key-value storage with values in Zp. All Zp-DOKVS are binary DOKVS, that is, Decode algorithm can be
 * simply written as y = &lt;v(x), D&gt;, where v(x) is the binary position, D is the DOKVS.
 *
 * @author Weiran Liu
 * @date 2024/2/19
 */
public interface ZpDokvs<T> {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    ZpDokvsType getType();

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
     * Gets the binary positions for the given key. All positions are in range [0, m). The positions is distinct.
     *
     * @param key the key.
     * @return the binary positions.
     */
    int[] positions(T key);

    /**
     * Gets the maximal position num.
     *
     * @return the maximal position num.
     */
    int maxPositionNum();

    /**
     * Encodes the key-value map.
     *
     * @param keyValueMap  key-value map.
     * @param doublyEncode encode with doubly obliviousness.
     * @return encoded storage.
     * @throws ArithmeticException if we cannot finish encoding.
     */
    BigInteger[] encode(Map<T, BigInteger> keyValueMap, boolean doublyEncode) throws ArithmeticException;

    /**
     * Decodes the key.
     *
     * @param storage encoded storage.
     * @param key     key.
     * @return value.
     */
    BigInteger decode(BigInteger[] storage, T key);

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
