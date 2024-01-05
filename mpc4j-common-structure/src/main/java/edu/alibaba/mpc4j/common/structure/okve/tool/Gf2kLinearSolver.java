package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import cc.redberry.rings.util.ArraysUtil;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.Consistent;
import static cc.redberry.rings.linear.LinearSolver.SystemInfo.Inconsistent;

/**
 * Solving the linear equation Ax = b, where A is a matrix with n×m GF(2^κ) elements, y is a vector with GF(2^κ) elements.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public class Gf2kLinearSolver {
    /**
     * GF(2^κ) instance
     */
    private final Gf2k gf2k;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public Gf2kLinearSolver(Gf2k gf2k) {
        this(gf2k, new SecureRandom());
    }

    public Gf2kLinearSolver(Gf2k gf2k, SecureRandom secureRandom) {
        this.gf2k = gf2k;
        this.secureRandom = secureRandom;
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs}. Note that here we only allow
     * <p> m (number of columns) >= n (number of rows) </p>
     *
     * @param lhs the lhs of the system.
     * @param rhs the rhs of the system.
     * @return the information for row Echelon form.
     */
    private RowEchelonFormInfo rowEchelonForm(byte[][][] lhs, byte[][] rhs) {
        int nRows = lhs.length;
        TIntSet maxLisColumns = new TIntHashSet(nRows);
        // do not need to solve when nRows = 0
        if (nRows == 0) {
            return new RowEchelonFormInfo(0, maxLisColumns);
        }
        int nColumns = lhs[0].length;
        // number of zero columns, here we consider if some columns are 0.
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            if (gf2k.isZero(lhs[row][iColumn])) {
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (!gf2k.isZero(lhs[iRow][iColumn])) {
                        max = iRow;
                        break;
                    }
                }
                ArraysUtil.swap(lhs, row, max);
                ArraysUtil.swap(rhs, row, max);
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (gf2k.isZero(lhs[row][iColumn])) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                byte[] alpha = gf2k.div(lhs[iRow][iColumn], lhs[row][iColumn]);
                rhs[iRow] = gf2k.sub(rhs[iRow], gf2k.mul(rhs[row], alpha));
                if (!gf2k.isZero(alpha)) {
                    for (int iCol = iColumn; iCol < nColumns; ++iCol) {
                        lhs[iRow][iCol] = gf2k.sub(lhs[iRow][iCol], gf2k.mul(alpha, lhs[row][iCol]));
                    }
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
     * @param lhs    the lhs of the system (will be reduced to row echelon form).
     * @param rhs    the rhs of the system.
     * @param result where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo freeSolve(byte[][][] lhs, byte[][] rhs, byte[][] result) {
        return solve(lhs, rhs, result, false);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as random. Note that lsh is modified when
     * solving the system.
     *
     * @param lhs    the lhs of the system (will be reduced to row echelon form).
     * @param rhs    the rhs of the system.
     * @param result where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo fullSolve(byte[][][] lhs, byte[][] rhs, byte[][] result) {
        return solve(lhs, rhs, result, true);
    }

    private SystemInfo solve(byte[][][] lhs, byte[][] rhs, byte[][] result, boolean isFull) {
        MathPreconditions.checkEqual("lhs.length", "rhs.length", lhs.length, rhs.length);
        int nRows = lhs.length;
        int nColumns = result.length;
        // m >= n
        MathPreconditions.checkGreaterOrEqual("m", nColumns, nRows);
        // verify each row has nColumns elements
        Arrays.stream(lhs).forEach(row ->
            MathPreconditions.checkEqual("row.length", "m", row.length, nColumns)
        );
        if (nRows == 0) {
            // if n = 0, all solutions are good.
            if (isFull) {
                // full random variables
                for (int iColumn = 0; iColumn < nColumns; iColumn++) {
                    result[iColumn] = gf2k.createNonZeroRandom(secureRandom);
                }
            } else {
                // full zero variables
                Arrays.fill(result, gf2k.createZero());
            }
            return Consistent;
        }
        if (nRows == 1) {
            return solveOneRow(lhs, rhs, result, isFull);
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFormInfo info = rowEchelonForm(lhs, rhs);
        int nUnderDetermined = info.getZeroColumnNum();
        Arrays.fill(result, gf2k.createZero());
        // for determined system, free and full solution are the same
        if (nUnderDetermined == 0 && nColumns == nRows) {
            for (int i = nColumns - 1; i >= 0; i--) {
                byte[] sum = gf2k.createZero();
                for (int j = i + 1; j < nColumns; j++) {
                    gf2k.addi(sum, gf2k.mul(result[j], lhs[i][j]));
                }
                result[i] = gf2k.div(gf2k.sub(rhs[i], sum), lhs[i][i]);
            }
            return Consistent;
        }
        return solveUnderDeterminedRows(lhs, rhs, result, info, isFull);
    }

    private SystemInfo solveOneRow(byte[][][] lhs, byte[][] rhs, byte[][] result, boolean isFull) {
        int nColumns = result.length;
        // when n = 1, then the linear system only has one equation a[0]x[0] + ... + a[m]x[m] = b[0]
        if (nColumns == 1) {
            // if m = 1, then we directly compute a[0]x[0] = b[0]
            if (!gf2k.isZero(lhs[0][0])) {
                // a[0] != 0, x[0] = b[0] / a[0]
                result[0] = gf2k.div(rhs[0], lhs[0][0]);
                return Consistent;
            } else {
                // a[0] == 0, it can be solved only if b[0] = 0
                if (gf2k.isZero(rhs[0])) {
                    result[0] = isFull ? gf2k.createNonZeroRandom(secureRandom) : gf2k.createZero();
                    return Consistent;
                } else {
                    return Inconsistent;
                }
            }
        }
        // if m > 1, the linear system a[0]x[0] + ... + a[m]x[m] = b[0] contains free variables.
        Arrays.fill(result, gf2k.createZero());
        // find the first non-zero a[t]
        int firstNonZeroColumn = -1;
        for (int i = 0; i < nColumns; ++i) {
            if (!gf2k.isZero(lhs[0][i])) {
                firstNonZeroColumn = i;
                break;
            }
        }
        // if all a[i] = 0, we have solution only if b[0] = 0
        if (firstNonZeroColumn == -1) {
            if (gf2k.isZero(rhs[0])) {
                if (isFull) {
                    // full random variables
                    for (int i = 0; i < nColumns; i++) {
                        result[i] = gf2k.createNonZeroRandom(secureRandom);
                    }
                }
                return Consistent;
            } else {
                // if all a[i] = 0 and b[0] != 0, this means we do not have any solution.
                return Inconsistent;
            }
        } else {
            // b[0] != 0, we need to consider the first non-zero a[t]
            if (isFull) {
                // set random variables
                for (int i = 0; i < nColumns; ++i) {
                    if (i == firstNonZeroColumn) {
                        continue;
                    }
                    // for i != t, set random x[i]
                    result[i] = gf2k.createNonZeroRandom(secureRandom);
                    if (!gf2k.isZero(lhs[0][i])) {
                        // a[i] != 0, b[0] = b[0] - a[i] * x[i].
                        gf2k.subi(rhs[0], gf2k.mul(lhs[0][i], result[i]));
                    }
                }
            }
            // set x[t] = b[0] / a[0]
            result[firstNonZeroColumn] = gf2k.div(rhs[0], lhs[0][firstNonZeroColumn]);
            return Consistent;
        }
    }

    private SystemInfo solveUnderDeterminedRows(byte[][][] lhs, byte[][] rhs, byte[][] result,
                                                             RowEchelonFormInfo info, boolean isFull) {
        int nRows = lhs.length;
        int nColumns = result.length;
        // back substitution in case of under-determined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        // number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            iRow = iColumn - nZeroColumns;
            if (gf2k.isZero(lhs[iRow][iColumn])) {
                if (iColumn == (nColumns - 1) && !gf2k.isZero(rhs[iRow])) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                // full solution needs to set the corresponding result[iColumn] as a random variable
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // scale current row, that is, make lhs[iRow][iColumn] = 1, and scale other entries in this row.
            byte[][] row = lhs[iRow];
            // we will modify row[iColumn], copy val
            byte[] val = BytesUtils.clone(row[iColumn]);
            byte[] valInv = gf2k.inv(val);
            for (int i = iColumn; i < nColumns; i++) {
                row[i] = gf2k.mul(valInv, row[i]);
            }
            rhs[iRow] = gf2k.mul(rhs[iRow], valInv);
            // scale all rows before
            for (int i = 0; i < iRow; i++) {
                byte[][] pRow = lhs[i];
                // we will modify pRow[iColumn], copy v
                byte[] v = BytesUtils.clone(pRow[iColumn]);
                if (gf2k.isZero(v)) {
                    continue;
                }
                for (int j = iColumn; j < nColumns; ++j) {
                    pRow[j] = gf2k.sub(pRow[j], gf2k.mul(v, row[j]));
                }
                gf2k.subi(rhs[i], gf2k.mul(rhs[iRow], v));
            }
            if (!gf2k.isZero(rhs[iRow]) && gf2k.isZero(lhs[iRow][iColumn])) {
                return Inconsistent;
            }
            // label that column and its corresponding row for the solution b[row].
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }
        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!gf2k.isZero(rhs[iRow])) {
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
                result[nonMaxLisColumn] = gf2k.createNonZeroRandom(secureRandom);
            }
            for (int i = 0; i < nzColumns.size(); ++i) {
                int iNzColumn = nzColumns.get(i);
                int iNzRow = nzRows.get(i);
                // subtract other free variables
                for (int nonMaxLisColumn : nonMaxLisColumnArray) {
                    if (!gf2k.isZero(lhs[iNzRow][nonMaxLisColumn])) {
                        result[iNzColumn] = gf2k.sub(result[iNzColumn], gf2k.mul(lhs[iNzRow][nonMaxLisColumn], result[nonMaxLisColumn]));
                    }
                }
            }
        }
        return Consistent;
    }
}
