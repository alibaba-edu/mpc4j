package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.util.ArraysUtil;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Given an n × m (n ≥ m) Zp matrix, it finds the max linear independent rows.
 *
 * @author Weiran Liu
 * @date 2021/09/11
 */
public class ZpMaxLisFinder {
    /**
     * Zp instance
     */
    private final Zp zp;

    public ZpMaxLisFinder(Zp zp) {
        this.zp = zp;
    }

    /**
     * Gives the row echelon form of the matrix A.
     *
     * @param lhs the lhs of the system.
     * @return the swapped row labels.
     */
    private int[] rowEchelonForm(BigInteger[][] lhs) {
        int nRows = lhs.length;
        MathPreconditions.checkPositive("n", nRows);
        int nColumns = lhs[0].length;
        // 0 <= m <= n
        MathPreconditions.checkNonNegativeInRangeClosed("m", nColumns, nRows);
        // verify each row has nColumns elements
        Arrays.stream(lhs).forEach(row ->
            MathPreconditions.checkEqual("row.length", "m", row.length, nColumns)
        );
        int[] rowLabels = IntStream.range(0, nRows).toArray();
        // number of zero columns, here we consider if the leading row is 0
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            // find the row where the first element is not 0
            if (zp.isZero(lhs[row][iColumn])) {
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (!zp.isZero(lhs[iRow][iColumn])) {
                        max = iRow;
                        break;
                    }
                }
                ArraysUtil.swap(lhs, row, max);
                // swap the row label
                int rowIndexTemp = rowLabels[row];
                rowLabels[row] = rowLabels[max];
                rowLabels[max] = rowIndexTemp;
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (zp.isZero(lhs[row][iColumn])) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                BigInteger alpha = zp.div(lhs[iRow][iColumn], lhs[row][iColumn]);
                if (!zp.isZero(alpha)) {
                    for (int iCol = iColumn; iCol < nColumns; ++iCol) {
                        lhs[iRow][iCol] = zp.sub(lhs[iRow][iCol], zp.mul(alpha, lhs[row][iCol]));
                    }
                }
            }
        }
        return rowLabels;
    }

    /**
     * Gets maximal linear independent columns. Note that lsh is not modified.
     *
     * @param lhs      the lhs of the system.
     * @return maximal linear independent rows.
     */
    public TIntSet getLisRows(BigInteger[][] lhs) {
        int nRows = lhs.length;
        MathPreconditions.checkPositive("n", nRows);
        int nColumns = lhs[0].length;
        // 0 <= m <= n
        MathPreconditions.checkNonNegativeInRangeClosed("m", nColumns, nRows);
        // verify each row has nColumns elements
        Arrays.stream(lhs).forEach(row ->
            MathPreconditions.checkEqual("row.length", "m", row.length, nColumns)
        );
        // copy the matrix
        BigInteger[][] copyLhs = BigIntegerUtils.clone(lhs);
        if (nRows == 1) {
            // if n = 1, and we know that any row cannot be all-zero, then this row is the only linear independent row.
            TIntSet hashSet = new TIntHashSet(1);
            hashSet.add(0);
            return hashSet;
        }
        // if n > 1, transform lsh to Echelon form.
        int[] rowLabels = rowEchelonForm(copyLhs);
        // there are at most n linear independent rows.
        TIntSet lisRowSet = new TIntHashSet(nRows);
        // number of zero columns
        int nZeroColumns = 0;
        int iRow;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find linear independent rows, note that lhs[iRow, iColumn]
            iRow = iColumn - nZeroColumns;
            // if this pivot is zero, and it is the last row, there is no solution
            if (zp.isZero(copyLhs[iRow][iColumn])) {
                if (iColumn == (nColumns - 1)) {
                    return lisRowSet;
                }
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            lisRowSet.add(rowLabels[iRow]);
        }
        return lisRowSet;
    }
}
