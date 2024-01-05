package edu.alibaba.mpc4j.common.structure.matrix.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.structure.matrix.Matrix;

import java.math.BigInteger;

/**
 * Zp matrix.
 *
 * @author Weiran Liu
 * @date 2023/6/19
 */
public interface ZpMatrix extends Matrix {
    /**
     * Gets Zp instance.
     *
     * @return Zp instance.
     */
    Zp getZp();

    /**
     * Gets the assigned row.
     *
     * @param iRow row index.
     * @return the assigned row.
     */
    BigInteger[] getRow(int iRow);

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
    BigInteger getEntry(int iRow, int iColumn);

    /**
     * Gets the data.
     *
     * @return the data.
     */
    BigInteger[][] getData();
}
