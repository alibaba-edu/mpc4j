package edu.alibaba.mpc4j.common.structure.vector;

/**
 * ring vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface RingVector extends Vector {
    /**
     * Addition.
     *
     * @param other the other vector.
     * @return the result.
     */
    RingVector add(RingVector other);

    /**
     * In-place addition.
     *
     * @param other the other vector.
     */
    void addi(RingVector other);

    /**
     * Negation.
     *
     * @return the result.
     */
    RingVector neg();

    /**
     * In-place negation.
     */
    void negi();

    /**
     * Subtraction.
     *
     * @param other the other vector.
     * @return the result.
     */
    RingVector sub(RingVector other);

    /**
     * In-place subtraction.
     *
     * @param other the other vector.
     */
    void subi(RingVector other);

    /**
     * Multiplication.
     *
     * @param other the other vector.
     * @return the result.
     */
    RingVector mul(RingVector other);

    /**
     * In-place multiplication.
     *
     * @param other the other vector.
     */
    void muli(RingVector other);
}
