package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import cc.redberry.rings.util.ArraysUtil;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.*;

/**
 * Solving the linear equation Ax = b, where A is a bit matrix represented in a compact form (using byte[][]), x is a
 * vector containing elements for which addition and subtraction are all XOR.
 *
 * @author Weiran Liu
 * @date 2023/6/16
 */
public class BinaryLinearSolver {
    /**
     * l
     */
    private final int l;
    /**
     * byte l
     */
    private final int byteL;
    /**
     * zero, only use for comparison
     */
    private final byte[] zeroElement;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public BinaryLinearSolver(int l) {
        this(l, new SecureRandom());
    }

    public BinaryLinearSolver(int l, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        zeroElement = new byte[byteL];
        Arrays.fill(zeroElement, (byte) 0x00);
        this.secureRandom = secureRandom;
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs}. Note that here we only allow
     * <p> m (number of columns) >= n (number of rows) </p>
     *
     * @param lhs      the lhs of the system.
     * @param nColumns number of columns.
     * @param rhs      the rhs of the system.
     * @return the information for row Echelon form.
     */
    private RowEchelonFormInfo rowEchelonForm(byte[][] lhs, int nColumns, byte[][] rhs) {
        int nRows = lhs.length;
        TIntSet maxLisColumns = new TIntHashSet(nRows);
        // do not need to solve when nRows = 0
        if (nRows == 0) {
            return new RowEchelonFormInfo(0, maxLisColumns);
        }
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // number of zero columns, here we consider if some columns are 0.
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
                ArraysUtil.swap(rhs, row, max);
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (!BinaryUtils.getBoolean(lhs[row], iColumn + nOffsetColumns)) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                boolean alpha = BinaryUtils.getBoolean(lhs[iRow], iColumn + nOffsetColumns);
                if (alpha) {
                    BytesUtils.xori(rhs[iRow], rhs[row]);
                    BytesUtils.xori(lhs[iRow], lhs[row]);
                }
            }
        }
        return new RowEchelonFormInfo(nZeroColumns, maxLisColumns);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as zero. Note that lsh is modified when solving
     * the system.
     *
     * @param lhs      the lhs of the system (will be reduced to row echelon form).
     * @param nColumns number of columns.
     * @param rhs      the rhs of the system.
     * @param result   where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo freeSolve(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result) {
        return solve(lhs, nColumns, rhs, result, false);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as random. Note that lsh is modified when
     * solving the system.
     *
     * @param lhs      the lhs of the system (will be reduced to row echelon form).
     * @param nColumns number of columns.
     * @param rhs      the rhs of the system.
     * @param result   where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo fullSolve(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result) {
        return solve(lhs, nColumns, rhs, result, true);
    }

    private SystemInfo solve(byte[][] lhs, int nColumns, byte[][] rhs, byte[][] result, boolean isFull) {
        MathPreconditions.checkEqual("lhs.length", "rhs.length", lhs.length, rhs.length);
        int nRows = lhs.length;
        // m >= n
        MathPreconditions.checkGreaterOrEqual("m", nColumns, nRows);
        MathPreconditions.checkEqual("result.length", "m", result.length, nColumns);
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // verify each row has at most nColumns valid bits
        Arrays.stream(lhs).forEach(row ->
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, nByteColumns, nColumns))
        );
        if (nRows == 0) {
            // if n = 0, all solutions are good.
            if (isFull) {
                // full random variables
                for (int iColumn = 0; iColumn < nColumns; iColumn++) {
                    result[iColumn] = createNonZeroRandom();
                }
            } else {
                // full zero variables
                Arrays.fill(result, createZero());
            }
            return Consistent;
        }
        if (nRows == 1) {
            return solveOneRow(lhs[0], rhs[0], result, isFull);
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFormInfo info = rowEchelonForm(lhs, nColumns, rhs);
        int nUnderDetermined = info.getZeroColumnNum();
        Arrays.fill(result, createZero());
        // for determined system, free and full solution are the same
        if (nUnderDetermined == 0 && nColumns == nRows) {
            for (int i = nRows - 1; i >= 0; i--) {
                byte[] sum = createZero();
                for (int j = i + 1; j < nColumns; j++) {
                    if (BinaryUtils.getBoolean(lhs[i], nOffsetColumns + j)) {
                        addi(sum, result[j]);
                    }
                }
                result[i] = sub(rhs[i], sum);
            }
            return Consistent;
        }
        return solveUnderDeterminedRows(lhs, rhs, result, info, isFull);
    }

    private SystemInfo solveOneRow(byte[] lh0, byte[] rh0, byte[][] result, boolean isFull) {
        int nColumns = result.length;
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // when n = 1, then the linear system only has one equation a[0]x[0] + ... + a[m]x[m] = b[0]
        if (nColumns == 1) {
            // if m = 1, then we directly compute a[0]x[0] = b[0]
            if (BinaryUtils.getBoolean(lh0, nOffsetColumns)) {
                // if a[0] = 1, then x[0] = b[0] / a[0] = b[0]
                result[0] = BytesUtils.clone(rh0);
                return Consistent;
            } else {
                // if a[0] = 0, it can be solved only if b[0] = 0
                if (isZero(rh0)) {
                    result[0] = isFull ? createNonZeroRandom() : createZero();
                    return Consistent;
                } else {
                    return Inconsistent;
                }
            }
        }
        // if m > 1, the linear system a[0]x[0] + ... + a[m]x[m] = b[0] contains free variables.
        Arrays.fill(result, new byte[byteL]);
        // find the first non-zero a[t]
        int firstNonZeroColumn = -1;
        for (int i = 0; i < nColumns; ++i) {
            if (BinaryUtils.getBoolean(lh0, nOffsetColumns + i)) {
                firstNonZeroColumn = i;
                break;
            }
        }
        // if all a[i] = 0, we have solution only if b[0] = 0
        if (firstNonZeroColumn == -1) {
            if (isZero(rh0)) {
                if (isFull) {
                    // full random variables
                    for (int i = 0; i < nColumns; i++) {
                        result[i] = createNonZeroRandom();
                    }
                }
                return Consistent;
            } else {
                // if all a[i] = 0 and b[0] != 0, this means we do not have any solution.
                return Inconsistent;
            }
        } else {
            // b[0] != 0, we need to consider the first non-zero equation a[t]x = b[t].
            if (isFull) {
                // set random variables
                for (int i = 0; i < nColumns; ++i) {
                    if (i == firstNonZeroColumn) {
                        continue;
                    }
                    // for i != t, set random x[i]
                    result[i] = createNonZeroRandom();
                    if (BinaryUtils.getBoolean(lh0, nOffsetColumns + i)) {
                        // a[i] != 0, b[0] = b[0] - a[i] * x[i] = b[0] - x[i].
                        subi(rh0, result[i]);
                    }
                }
            }
            // set x[t] = b[0] / a[0] = b[0]
            result[firstNonZeroColumn] = BytesUtils.clone(rh0);
            return Consistent;
        }
    }

    private SystemInfo solveUnderDeterminedRows(byte[][] lhs, byte[][] rhs, byte[][] result,
                                                RowEchelonFormInfo info, boolean isFull) {
        int nRows = lhs.length;
        int nColumns = result.length;
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        // back substitution in case of under-determined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        // number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            iRow = iColumn - nZeroColumns;
            if (!BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns + iColumn)) {
                if (iColumn == (nColumns - 1) && !isZero(rhs[iRow])) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                // full solution needs to set the corresponding result[iColumn] as a random variable
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // scale current row, that is, make lhs[iRow][iColumn] = 1, and scale other entries in this row.
            // it should be as follows, but here val == 1, valInv = 1, so we can ignore these steps
            // val = row[iColumn]; valInv = 1 / val;
            // for (int i = iColumn; i < nColumns; i++) { row[i] = valInv * row[i] }
            byte[] row = lhs[iRow];
            // scale all rows before
            for (int i = 0; i < iRow; i++) {
                byte[] pRow = lhs[i];
                boolean v = BinaryUtils.getBoolean(pRow, nOffsetColumns + iColumn);
                if (!v) {
                    continue;
                }
                BytesUtils.xori(pRow, row);
                subi(rhs[i], rhs[iRow]);
            }
            if (!isZero(rhs[iRow]) && !BinaryUtils.getBoolean(lhs[iRow], nOffsetColumns + iColumn)) {
                return Inconsistent;
            }
            // label that column and its corresponding row for the solution b[row].
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }
        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!isZero(rhs[iRow])) {
                    return Inconsistent;
                }
            }
        }
        for (int i = 0; i < nzColumns.size(); ++i) {
            result[nzColumns.get(i)] = rhs[nzRows.get(i)];
        }
        if (isFull) {
            TIntSet maxLisColumns = info.getMaxLisColumns();
            TIntSet nonMaxLisColumns = new TIntHashSet(nColumns);
            nonMaxLisColumns.addAll(IntStream.range(0, nColumns).toArray());
            nonMaxLisColumns.removeAll(maxLisColumns);
            int[] nonMaxLisColumnArray = nonMaxLisColumns.toArray();
            // set result[iColumn] corresponding to the non-maxLisColumns as random variables
            for (int nonMaxLisColumn : nonMaxLisColumnArray) {
                result[nonMaxLisColumn] = createNonZeroRandom();
            }
            for (int i = 0; i < nzColumns.size(); ++i) {
                int iNzColumn = nzColumns.get(i);
                int iNzRow = nzRows.get(i);
                // subtract other free variables
                for (int nonMaxLisColumn : nonMaxLisColumnArray) {
                    if (BinaryUtils.getBoolean(lhs[iNzRow], nOffsetColumns + nonMaxLisColumn)) {
                        subi(result[iNzColumn], result[nonMaxLisColumn]);
                    }
                }
            }
        }
        return Consistent;
    }

    private boolean isZero(byte[] element) {
        return Arrays.equals(zeroElement, element);
    }

    private byte[] createZero() {
        return new byte[byteL];
    }

    private byte[] createNonZeroRandom() {
        byte[] element;
        do {
            element = BytesUtils.randomByteArray(byteL, l, secureRandom);
        } while (isZero(element));
        return element;
    }

    private void addi(byte[] p, byte[] q) {
        BytesUtils.xori(p, q);
    }

    private byte[] sub(byte[] p, byte[] q) {
        return BytesUtils.xor(p, q);
    }

    private void subi(byte[] p, byte[] q) {
        BytesUtils.xori(p, q);
    }
}
