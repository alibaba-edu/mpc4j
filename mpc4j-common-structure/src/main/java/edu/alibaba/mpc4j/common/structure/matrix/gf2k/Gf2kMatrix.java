package edu.alibaba.mpc4j.common.structure.matrix.gf2k;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.structure.matrix.Matrix;

/**
 * GF(2^κ) matrix.
 *
 * @author Weiran Liu
 * @date 2023/7/4
 */
public interface Gf2kMatrix extends Matrix {
    /**
     * Gets GF2K instance.
     *
     * @return GF2K instance.
     */
    Gf2k getGf2k();

    /**
     * Gets the assigned row.
     *
     * @param iRow row index.
     * @return the assigned row.
     */
    byte[][] getRow(int iRow);

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
     * @param iRow    row index.
     * @param iColumn column index.
     * @return the entry at (iRow, iColumn).
     */
    byte[] getEntry(int iRow, int iColumn);

    /**
     * Gets the data.
     *
     * @return the data.
     */
    byte[][][] getData();
}
