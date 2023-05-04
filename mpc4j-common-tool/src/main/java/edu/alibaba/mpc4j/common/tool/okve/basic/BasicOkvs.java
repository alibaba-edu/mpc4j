package edu.alibaba.mpc4j.common.tool.okve.basic;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Basic OKVS. The OKVS definition comes from:
 * <p>
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 * </p>
 * Basic OKVS has the property that keys and values must be in the same field, i.e, they must have the same bit length.
 *
 * @author Weiran Liu
 * @date 2023/3/27
 */
public interface BasicOkvs {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    BasicOkvsFactory.BasicOkvsType getType();

    /**
     * Sets using parallel encode.
     *
     * @param parallelEncode parallel encode or not.
     */
    void setParallelEncode(boolean parallelEncode);

    /**
     * Gets using  parallel encode.
     * @return parallel encode or not.
     */
    boolean getParallelEncode();

    /**
     * Encodes the key-value map.
     *
     * @param keyValueMap the key-value map.
     * @return the OKVS storage.
     */
    byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap);

    /**
     * Decodes the value.
     *
     * @param storage the OKVS storage.
     * @param key     the key.
     * @return the value.
     */
    byte[] decode(byte[][] storage, ByteBuffer key);

    /**
     * Gets the number of allowed key-value pairs.
     *
     * @return the number of allowed key-value pairs.
     */
    int getN();

    /**
     * Gets the key / value bit length, which must satisfies {@code l % Byte.SIZE == 0}.
     *
     * @return the key / value bit length.
     */
    int getL();

    /**
     * Gets the key / value byte length.
     *
     * @return the key / value byte length.
     */
    int getByteL();

    /**
     * Gets the OKVS storage size.
     *
     * @return the OKVS storage size.
     */
    int getM();

    /**
     * Gets the OKVS encoding rate, i.e., n / m.
     *
     * @return the OKVS encoding rate.
     */
    default double rate() {
        return ((double) getN()) / getM();
    }

    /**
     * Gets the negative log failure probability. If the failure probability is p, then return -log_2(p).
     *
     * @return the negative log failure probability.
     */
    int getNegLogFailureProbability();
}
