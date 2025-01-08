package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.structure.matrix.Matrix;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;

/**
 * F2 -> F3 weak PRF matrix.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public interface F23WprfMatrix extends Matrix {
    /**
     * rows
     */
    int ROWS = F23Wprf.M;
    /**
     * row in binary bytes
     */
    int ROW_BINARY_BYTES = ROWS / Byte.SIZE;
    /**
     * rows in bytes
     */
    int ROW_BYTES = ROWS / (Byte.SIZE / 2);
    /**
     * rows in longs
     */
    int ROW_LONGS = ROWS / (Long.SIZE / 2);
    /**
     * columns
     */
    int COLUMNS = F23Wprf.T;

    /**
     * Left Multiplication, where vector contains Z_2 elements.
     *
     * @param vector the vector.
     * @return the result.
     */
    byte[] leftBinaryMul(byte[] vector);

    /**
     * Left multiplication, where vector contains Z_3 elements.
     *
     * @param vector the vector.
     * @return the result.
     */
    byte[] leftMul(byte[] vector);

    /**
     * Left multiplication, where vector contains compressed Z_3 elements.
     *
     * @param vector the vector.
     * @return the result.
     */
    byte[] leftCompressMul(byte[] vector);

    /**
     * Left multiplication, where vector contains compressed Z_3 elements.
     *
     * @param vector the vector.
     * @return the result.
     */
    byte[] leftCompressMul(long[] vector);

    /**
     * Gets the matrix type.
     *
     * @return matrix type.
     */
    F23WprfMatrixType getType();
}
