package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import java.util.Map;

/**
 * doubly oblivious key-value storage with values in GF(2^e). DOKVS is implicitly defined in the following paper:
 * <p>
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. EUROCRYPT 2021. Springer, Cham, pp.
 * 901-930.
 * </p>
 * Here, we refer to the following paper to define DOKVS:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. ACM CCS 2022,
 * pp. 2505-2517.
 * </p>
 * An OKVS scheme is doubly oblivious if it is an OKVS scheme and if for all $k_i ∈ K, r ∈ {0, 1}^κ, the output OKVS D
 * is the uniform distribution over V^m.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public interface Gf2eDokvs<T> {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    Gf2eDokvsFactory.Gf2eDokvsType getType();

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
     * Gets the bit length of values, i.e., all values should be in {0, 1}^l.
     *
     * @return the bit length of values.
     */
    int getL();

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
