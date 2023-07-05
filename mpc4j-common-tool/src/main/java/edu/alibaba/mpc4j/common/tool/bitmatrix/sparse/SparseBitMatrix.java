package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;

/**
 * sparse bit matrix.
 *
 * @author Weiran Liu
 * @date 2023/6/25
 */
public interface SparseBitMatrix {
    /**
     * Left-multiplies the boolean vector x with each column in M, i.e., computes x·M.
     *
     * @param x the boolean vector x.
     * @return the result.
     */
    boolean[] lmul(final boolean[] x);

    /**
     * Left-multiplies the boolean vector x with each column in M, and then xor the result into the other boolean vector,
     * i.e., computes y = x·M ⊕ y.
     *
     * @param x the boolean vector x.
     * @param y the boolean vector y.
     */
    void lmulAddi(final boolean[] x, boolean[] y);

    /**
     * Left-multiplies the GF2L vector with each column in M, i.e., computes x·M by treating each entry in M as 1's
     * in the GF2L field.
     *
     * @param x the GF2L vector x.
     * @return the result.
     */
    byte[][] lExtMul(final byte[][] x);

    /**
     * Left-multiplies the GF2L vector with each column in M, and then xor the result into the other GF2L vector,
     * i.e., computes y = x·M ⊕ y by treating each entry in M as 1's in the GF2L field.
     *
     * @param x the GF2L vector x.
     * @param y the GF2L vector y.
     */
    void lExtMulAddi(final byte[][] x, byte[][] y);

    /**
     * Gets the number of rows.
     *
     * @return the number of rows.
     */
    int getRows();

    /**
     * Gets the number of columns.
     *
     * @return the number of columns.
     */
    int getColumns();

    /**
     * Gets the assigned column.
     *
     * @param index the index.
     * @return the assigned column.
     */
    SparseBitVector getColumn(int index);

    /**
     * Gets the size. Note that only square matrix support this.
     *
     * @return size.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    int getSize();

    /**
     * Gets the entry at (iRow, iColumn).
     *
     * @param x row index.
     * @param y column index.
     * @return the entry at (iRow, iColumn).
     */
    boolean get(int x, int y);

    /**
     * to dense bit matrix.
     *
     * @return dense bit matrix
     */
    DenseBitMatrix toDense();

    /**
     * Transposes a matrix to a dense matrix.
     *
     * @return result.
     */
    DenseBitMatrix transposeDense();
}
