package edu.alibaba.mpc4j.common.structure.matrix;

import edu.alibaba.mpc4j.common.structure.vector.RingVector;

/**
 * ring matrix.
 *
 * @author Liqiang Peng
 * @date 2023/5/23
 */
public interface LongRingMatrix extends Matrix {

    /**
     * Get the element at position (i, j).
     *
     * @param i row index.
     * @param j col index.
     * @return element.
     */
    long get(int i, int j);

    /**
     * Set the element at position (i, j).
     *
     * @param i       row index.
     * @param j       col index.
     * @param element value.
     */
    void set(int i, int j, long element);

    /**
     * Append rows with zero elements.
     *
     * @param n row num.
     * @return Appended matrix.
     */
    LongRingMatrix appendZeros(int n);

    /**
     * Concat.
     *
     * @param other the other matrix.
     * @return the result.
     */
    LongRingMatrix concat(LongRingMatrix other);

    /**
     * Addition.
     *
     * @param element the element.
     */
    void add(long element);

    /**
     * Addition.
     *
     * @param other the other matrix.
     * @return the result.
     */
    LongRingMatrix matrixAdd(LongRingMatrix other);

    /**
     * Addition at position (i, j).
     *
     * @param element the element.
     * @param i       row index.
     * @param j       col index.
     */
    void addAt(long element, int i, int j);

    /**
     * Subtraction.
     *
     * @param other the other matrix.
     * @return the result.
     */
    LongRingMatrix matrixSub(LongRingMatrix other);

    /**
     * Subtraction.
     *
     * @param element the element.
     */
    void sub(long element);

    /**
     * Multiplication.
     *
     * @param other the other matrix.
     * @return the result.
     */
    LongRingMatrix matrixMul(LongRingMatrix other);

    /**
     * Multiplication.
     *
     * @param vector the vector.
     * @return the result.
     */
    RingVector matrixMulVector(RingVector vector);

    /**
     * Transposition.
     *
     * @return the result.
     */
    LongRingMatrix transpose();

    /**
     * Decompose the matrix base on p.
     *
     * @param p the modulo.
     * @return decomposed matrix.
     */
    LongRingMatrix decompose(long p);

    /**
     * Recompose the matrix base on p.
     *
     * @param p the modulo.
     * @return recomposed matrix.
     */
    LongRingMatrix recompose(long p);
}
