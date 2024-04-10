package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;

/**
 * SIMD transpose bit matrix.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
public interface SimdTransBitMatrix {
    /**
     * Gets boolean value at (x, y).
     *
     * @param x x coordinate.
     * @param y y coordinate.
     * @return boolean value at (x, y).
     */
    boolean get(int x, int y);

    /**
     * Gets the {@code y}-th column.
     *
     * @param y y coordinate.
     * @return the {@code y}-th column.
     */
    byte[] getColumn(int y);

    /**
     * Sets the {@code y}-th column.
     *
     * @param y         y coordinate.
     * @param byteArray given {@code y}-th column.
     */
    void setColumn(int y, byte[] byteArray);

    /**
     * Gets number of rows.
     *
     * @return number of rows.
     */
    int getRows();

    /**
     * Gets number of columns.
     *
     * @return number of columns.
     */
    int getColumns();

    /**
     * Transpose the bit matrix.
     *
     * @return transposed result.
     */
    SimdTransBitMatrix transpose();

    /**
     * Gets type.
     *
     * @return type.
     */
    SimdTransBitMatrixType getType();
}
