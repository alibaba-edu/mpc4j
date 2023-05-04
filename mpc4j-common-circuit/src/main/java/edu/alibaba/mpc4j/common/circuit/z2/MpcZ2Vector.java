package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Mpc Bit Vector.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public interface MpcZ2Vector extends MpcVector {
    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    BitVector getBitVector();

    /**
     * Gets the num in bytes.
     *
     * @return the num in bytes.
     */
    int getByteNum();

    /**
     * Get the value at the index.
     *
     * @param index the index.
     * @return the value at the index.
     */
    boolean get(int index);
}
