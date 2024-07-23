package edu.alibaba.mpc4j.common.structure.vector;

/**
 * Field vector.
 *
 * @author Weiran Liu
 * @date 2024/5/25
 */
public interface FieldVector extends RingVector {
    /**
     * Inversion.
     *
     * @return the result.
     */
    FieldVector inv();

    /**
     * In-place inversion.
     */
    void invi();

    /**
     * Division.
     *
     * @param other the other vector.
     * @return the result.
     */
    FieldVector div(FieldVector other);

    /**
     * In-place division.
     *
     * @param other the other vector.
     */
    void divi(FieldVector other);
}
