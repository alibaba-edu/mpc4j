package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.util.ArraysUtil;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Given an n × m (n ≥ m) bit matrix, it finds the max linear independent rows.
 *
 * @author Weiran Liu
 * @date 2023/6/16
 */
public class BinaryMaxLisFinder {

    public BinaryMaxLisFinder() {
        // empty
    }

    /**
     * Gives the row echelon form of the matrix A.
     *
     * @param lhs      the lhs of the system.
     * @param nColumns number of columns.
     * @return the swapped row labels.
     */
    private int[] rowEchelonForm(byte[][] lhs, int nColumns) {
        int nRows = lhs.length;
        MathPreconditions.checkPositive("n", nRows);
        // 0 <= m <= n
        MathPreconditions.checkNonNegativeInRangeClosed("m", nColumns, nRows);
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // verify each row has at most nColumns valid bits, and not all-zero
        Arrays.stream(lhs).forEach(row ->
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, nByteColumns, nColumns))
        );
        int[] rowLabels = IntStream.range(0, nRows).toArray();
        // number of zero columns, here we consider if the leading row is 0
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            // find the row where the first bit is not 0
            if (!BinaryUtils.getBoolean(lhs[row], iColumn + nOffsetColumns)) {
                // if we find one, swap
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (BinaryUtils.getBoolean(lhs[iRow], iColumn + nOffsetColumns)) {
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
            if (!BinaryUtils.getBoolean(lhs[row], iColumn + nOffsetColumns)) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                boolean alpha = BinaryUtils.getBoolean(lhs[iRow], iColumn + nOffsetColumns);
                if (alpha) {
                    BytesUtils.xori(lhs[iRow], lhs[row]);
                }
            }
        }
        return rowLabels;
    }

    /**
     * Gets maximal linear independent columns. Note that lsh is not modified.
     *
     * @param lhs      the lhs of the system.
     * @param nColumns number of columns.
     * @return maximal linear independent rows.
     */
    public TIntSet getLisRows(byte[][] lhs, int nColumns) {
        int nRows = lhs.length;
        MathPreconditions.checkPositive("n", nRows);
        // 0 <= m <= n
        MathPreconditions.checkNonNegativeInRangeClosed("m", nColumns, nRows);
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // verify each row has at most nColumns valid bits, and not all-zero
        Arrays.stream(lhs).forEach(row ->
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, nByteColumns, nColumns))
        );
        // copy the matrix
        byte[][] copyLhs = BytesUtils.clone(lhs);
        if (nRows == 1) {
            // if n = 1, and we know that any row cannot be all-zero, then this row is the only linear independent row.
            TIntSet hashSet = new TIntHashSet(1);
            hashSet.add(0);
            return hashSet;
        }
        // if n > 1, transform lsh to Echelon form.
        int[] rowLabels = rowEchelonForm(copyLhs, nColumns);
        // there are at most n linear independent rows.
        TIntSet lisRowSet = new TIntHashSet(nRows);
        // number of zero columns
        int nZeroColumns = 0;
        int iRow;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find linear independent rows, note that lhs[iRow, iColumn]
            iRow = iColumn - nZeroColumns;
            // if this pivot is zero, and it is the last row, there is no solution
            if (!BinaryUtils.getBoolean(copyLhs[iRow], nOffsetColumns + iColumn)) {
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
