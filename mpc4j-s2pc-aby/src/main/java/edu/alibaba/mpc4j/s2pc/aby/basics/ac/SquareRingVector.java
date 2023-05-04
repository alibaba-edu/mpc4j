package edu.alibaba.mpc4j.s2pc.aby.basics.ac;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.RingVector;

/**
 * Secret-shared ring vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface SquareRingVector extends MpcVector {
    /**
     * Addition.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     * @return the result.
     */
    SquareRingVector add(SquareRingVector other, boolean plain);

    /**
     * In-place addition.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     */
    void addi(SquareRingVector other, boolean plain);

    /**
     * Negation.
     *
     * @param plain the result plain state.
     * @return the result.
     */
    SquareRingVector neg(boolean plain);

    /**
     * In-place negation.
     *
     * @param plain the result plain state.
     */
    void negi(boolean plain);

    /**
     * Subtraction.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     * @return the result.
     */
    SquareRingVector sub(SquareRingVector other, boolean plain);

    /**
     * In-place subtraction.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     */
    void subi(SquareRingVector other, boolean plain);

    /**
     * Multiplication.
     *
     * @param other the other vector.
     * @return the result.
     */
    SquareRingVector mul(SquareRingVector other);

    /**
     * In-place multiplication.
     *
     * @param other the other vector.
     */
    void muli(SquareRingVector other);

    /**
     * Gets the vector.
     *
     * @return the vector.
     */
    RingVector getVector();
}
