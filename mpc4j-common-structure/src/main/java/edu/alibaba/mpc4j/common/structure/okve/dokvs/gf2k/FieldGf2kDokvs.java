package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

/**
 * field GF2K-DOKVS
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public interface FieldGf2kDokvs<T> extends Gf2kDokvs<T> {
    /**
     * Gets the sparse positions.
     *
     * @param key key.
     * @return the sparse positions.
     */
    int[] sparsePositions(T key);

    /**
     * Gets the dense fields for the given key.
     *
     * @param key the key.
     * @return the dense fields.
     */
    byte[][] denseFields(T key);
}
