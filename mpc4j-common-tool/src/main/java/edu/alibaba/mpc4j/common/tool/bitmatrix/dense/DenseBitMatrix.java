package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * dense matrix.
 *
 * @author Weiran Liu
 * @date 2022/8/1
 */
public interface DenseBitMatrix {
    /**
     * xor a matrix.
     *
     * @param that that matrix.
     * @return added matrix.
     */
    DenseBitMatrix xor(DenseBitMatrix that);

    /**
     * Inplace xor a matrix.
     *
     * @param that that matrix.
     */
    void xori(DenseBitMatrix that);

    /**
     * Multiplies a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    DenseBitMatrix multiply(DenseBitMatrix that);

    /**
     * Left-multiplies a vector v (encoded as a byte array), i.e., computes v·M.
     *
     * @param v the vector v (encoded as a byte array).
     * @return v·M (encoded as a byte array).
     */
    byte[] leftMultiply(final byte[] v);

    /**
     * Left-multiplies a vector v (encoded as a byte array), and inplace add t, i.e., computes t = v·M ⊕ t.
     *
     * @param v the vector v (encoded as a byte array).
     * @param t the vector t (encoded as a byte array).
     */
    void leftMultiplyXori(final byte[] v, byte[] t);

    /**
     * Left-multiplies a vector v, i.e., computes v·M.
     *
     * @param v the vector v.
     * @return v·M.
     */
    boolean[] leftMultiply(final boolean[] v);

    /**
     * Left-multiplies a vector v, and inplace add t, i.e., computes t = v·M ⊕ t.
     *
     * @param v the vector v.
     * @param t the vector t.
     */
    void leftMultiplyXori(final boolean[] v, boolean[] t);

    /**
     * Left-multiplies an GF2L vector v, i.e., computes v·M by treating entries in M as 1's in the GF2L field.
     *
     * @param v an GF2L vector v.
     * @return v·M.
     */
    byte[][] leftGf2lMultiply(final byte[][] v);

    /**
     * Computes v·M, and inplace add the result in t, i.e., computes t = v·M ⊕ t by treating entries in M as 1's in
     * the GF2L field.
     *
     * @param v an GF2L vector v.
     * @param t an GF2L vector t.
     */
    void leftGf2lMultiplyXori(final byte[][] v, byte[][] t);

    /**
     * Transposes a matrix.
     *
     * @param envType  environment.
     * @param parallel parallel operation.
     * @return result.
     */
    DenseBitMatrix transpose(EnvType envType, boolean parallel);

    /**
     * Inverses the matrix.
     *
     * @return the inverse matrix.
     * @throws IllegalArgumentException if the matrix is not a square matrix.
     * @throws ArithmeticException      if the square matrix is not invertible.
     */
    DenseBitMatrix inverse();

    /**
     * Gets the number of rows.
     *
     * @return the number of rows.
     */
    int getRows();

    /**
     * Gets the assigned byte array row.
     *
     * @param iRow row index.
     * @return the assigned byte array row.
     */
    byte[] getByteArrayRow(int iRow);

    /**
     * Gets the assigned long array row.
     *
     * @param iRow row index.
     * @return the assigned long array row.
     */
    long[] getLongArrayRow(int iRow);

    /**
     * Gets the number of columns.
     *
     * @return the number of columns.
     */
    int getColumns();

    /**
     * Gets the size. Note that only square matrix support this.
     *
     * @return size.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    int getSize();

    /**
     * Gets the size in byte. Note that only square matrix support this.
     *
     * @return size.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    int getByteSize();

    /**
     * Gets the entry at (iRow, iColumn).
     *
     * @param x row index.
     * @param y column index.
     * @return the entry at (iRow, iColumn).
     */
    boolean get(int x, int y);

    /**
     * Gets the byte array data.
     *
     * @return the byte array data.
     */
    byte[][] getByteArrayData();

    /**
     * Gets the long array data.
     *
     * @return the long array data.
     */
    long[][] getLongArrayData();
}
