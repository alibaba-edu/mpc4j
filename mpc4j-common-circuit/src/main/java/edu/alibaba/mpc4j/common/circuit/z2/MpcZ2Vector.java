package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.stream.IntStream;

/**
 * MPC Bit Vector.
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
     * Gets the number of bit shares.
     *
     * @return the number of bit shares.
     */
    default int bitNum() {
        return getNum();
    }

    /**
     * Gets the num in bytes.
     *
     * @return the num in bytes.
     */
    int byteNum();

    /**
     * Reverse the inputs.
     *
     * @param inputs the inputs.
     * @return the reversed inputs.
     */
    static MpcZ2Vector[] reverse(MpcZ2Vector[] inputs) {
        MpcZ2Vector[] result = new MpcZ2Vector[inputs.length];
        IntStream.range(0, inputs.length).forEach(i -> result[i] = inputs[inputs.length - i - 1]);
        return result;
    }
}
