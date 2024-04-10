package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import cc.redberry.rings.util.ArraysUtil;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.*;

/**
 * Solving the linear equation Ax = b, where A is a matrix with n×m Zp elements, y is a vector with ECC elements.
 * <p></p>
 * The implementation is referred to cc.redberry.rings.linear.LinearSolver.
 *
 * @author Weiran Liu
 * @date 2021/05/08
 */
public class EccLinearSolver {
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public EccLinearSolver(Ecc ecc) {
        this(ecc, new SecureRandom());
    }

    public EccLinearSolver(Ecc ecc, SecureRandom secureRandom) {
        this.ecc = ecc;
        this.secureRandom = secureRandom;
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs}.
     *
     * @param lhs                    the lhs of the system.
     * @param rhs                    the rhs of the system.
     * @return the number of free variables.
     */
    private RowEchelonFormInfo rowEchelonForm(BigInteger[][] lhs, ECPoint[] rhs) {
        int nRows = lhs.length;
        TIntSet maxLisColumns = new TIntHashSet(nRows);
        // do not need to solve when nRows = 0
        if (nRows == 0) {
            return new RowEchelonFormInfo(0, maxLisColumns);
        }
        int nColumns = lhs[0].length;
        BigInteger n = ecc.getN();
        // number of zero columns, here we consider if some columns are 0.
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            if (lhs[row][iColumn].equals(BigInteger.ZERO)) {
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (!lhs[iRow][iColumn].equals(BigInteger.ZERO)) {
                        max = iRow;
                        break;
                    }
                }
                ArraysUtil.swap(lhs, row, max);
                if (rhs != null) {
                    ArraysUtil.swap(rhs, row, max);
                }
            }
            // singular
            if (lhs[row][iColumn].equals(BigInteger.ZERO)) {
                // nothing to do on this column
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                BigInteger alpha = lhs[iRow][iColumn].multiply(lhs[row][iColumn].modInverse(n));
                if (rhs != null) {
                    rhs[iRow] = rhs[iRow].subtract(rhs[row].multiply(alpha));
                }
                if (!alpha.equals(BigInteger.ZERO)) {
                    for (int iCol = iColumn; iCol < nColumns; ++iCol) {
                        lhs[iRow][iCol] = lhs[iRow][iCol].subtract(alpha.multiply(lhs[row][iCol]).mod(n)).mod(n);
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
    public LinearSolver.SystemInfo freeSolve(BigInteger[][] lhs, ECPoint[] rhs, ECPoint[] result) {
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
    public LinearSolver.SystemInfo fullSolve(BigInteger[][] lhs, ECPoint[] rhs, ECPoint[] result) {
        return solve(lhs, rhs, result, true);
    }

    private SystemInfo solve(BigInteger[][] lhs, ECPoint[] rhs, ECPoint[] result, boolean isFull) {
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
                    result[iColumn] = ecc.randomPoint(secureRandom);
                }
            } else {
                // full zero variables
                Arrays.fill(result, ecc.getInfinity());
            }
            return Consistent;
        }
        if (nRows == 1) {
            return solveOneRow(lhs, rhs, result, isFull);
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFormInfo info = rowEchelonForm(lhs, rhs);
        int nUnderDetermined = info.getZeroColumnNum();
        ECPoint infinity = ecc.getInfinity();
        BigInteger n = ecc.getN();
        Arrays.fill(result, infinity);
        // for determined system, free and full solution are the same
        if (nUnderDetermined == 0 && nColumns == nRows) {
            for (int i = nRows - 1; i >= 0; i--) {
                ECPoint sum = infinity;
                for (int j = i + 1; j < nColumns; j++) {
                    sum = sum.add(result[j].multiply(lhs[i][j]));
                }
                result[i] = rhs[i].subtract(sum).multiply(lhs[i][i].modInverse(n)).normalize();
            }
            return Consistent;
        }
        return solveUnderDeterminedRows(lhs, rhs, result, info, isFull);
    }

    private LinearSolver.SystemInfo solveOneRow(BigInteger[][] lhs, ECPoint[] rhs, ECPoint[] result, boolean isFull) {
        int nColumns = result.length;
        BigInteger n = ecc.getN();
        ECPoint infinity = ecc.getInfinity();
        // when n = 1, then the linear system only has one equation a[0]x[0] + ... + a[m]x[m] = b[0]
        if (nColumns == 1) {
            // if m = 1, then we directly compute a[0]x[0] = b[0]
            if (!lhs[0][0].equals(BigInteger.ZERO)) {
                // a[0] != 0, x[0] = b[0] / a[0]
                result[0] = rhs[0].multiply(lhs[0][0].modInverse(n));
                return Consistent;
            } else {
                // a[0] == 0, it can be solved only if b[0] = 0
                if (rhs[0].equals(infinity)) {
                    result[0] = isFull ? ecc.randomPoint(secureRandom) : infinity;
                    return Consistent;
                } else {
                    return Inconsistent;
                }
            }
        }
        // if m > 1, the linear system a[0]x[0] + ... + a[m]x[m] = b[0] contains free variables.
        Arrays.fill(result, infinity);
        // find the first non-zero a[t]
        int firstNonZeroColumn = -1;
        for (int i = 0; i < nColumns; ++i) {
            if (!lhs[0][i].equals(BigInteger.ZERO)) {
                firstNonZeroColumn = i;
                break;
            }
        }
        // if all a[i] = 0, we have solution only if b[0] = 0
        if (firstNonZeroColumn == -1) {
            if (rhs[0].equals(infinity)) {
                if (isFull) {
                    // full random variables
                    for (int i = 0; i < nColumns; i++) {
                        result[i] = ecc.randomPoint(secureRandom);
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
                    result[i] = ecc.randomPoint(secureRandom);
                    if (!lhs[0][i].equals(BigInteger.ZERO)) {
                        // a[i] != 0, b[0] = b[0] - a[i] * x[i].
                        rhs[0] = ecc.subtract(rhs[0], ecc.multiply(result[i], lhs[0][i]));
                    }
                }
            }
            // set x[t] = b[0] / a[0]
            result[firstNonZeroColumn] = ecc.multiply(rhs[0], lhs[0][firstNonZeroColumn].modInverse(n));
            return Consistent;
        }
    }

    private LinearSolver.SystemInfo solveUnderDeterminedRows(BigInteger[][] lhs, ECPoint[] rhs, ECPoint[] result,
                                                             RowEchelonFormInfo info, boolean isFull) {
        int nRows = lhs.length;
        int nColumns = result.length;
        ECPoint infinity = ecc.getInfinity();
        BigInteger n = ecc.getN();
        // back substitution in case of underdetermined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        // number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            iRow = iColumn - nZeroColumns;
            if (lhs[iRow][iColumn].equals(BigInteger.ZERO)) {
                if (iColumn == (nColumns - 1) && !rhs[iRow].equals(infinity)) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                // full solution needs to set the corresponding result[iColumn] as a random variable
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // scale current row
            BigInteger[] row = lhs[iRow];
            BigInteger val = row[iColumn];
            BigInteger valInv = val.modInverse(n);
            for (int i = iColumn; i < nColumns; i++) {
                row[i] = valInv.multiply(row[i]).mod(n);
            }
            rhs[iRow] = rhs[iRow].multiply(valInv);

            // scale all rows before
            for (int i = 0; i < iRow; i++) {
                BigInteger[] pRow = lhs[i];
                BigInteger v = pRow[iColumn];
                if (v.equals(BigInteger.ZERO)) {
                    continue;
                }
                for (int j = iColumn; j < nColumns; ++j) {
                    pRow[j] = pRow[j].subtract(v.multiply(row[j]).mod(n)).mod(n);
                }
                rhs[i] = rhs[i].subtract(rhs[iRow].multiply(v));
            }
            if (!rhs[iRow].equals(infinity) && lhs[iRow][iColumn].equals(BigInteger.ZERO)) {
                return Inconsistent;
            }
            // label that column and its corresponding row for the solution b[row].
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }
        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!rhs[iRow].equals(infinity)) {
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
                result[nonMaxLisColumn] = ecc.randomPoint(secureRandom);
            }
            for (int i = 0; i < nzColumns.size(); ++i) {
                int iNzColumn = nzColumns.get(i);
                int iNzRow = nzRows.get(i);
                // subtract other free variables
                for (int nonMaxLisColumn : nonMaxLisColumnArray) {
                    if (!lhs[iNzRow][nonMaxLisColumn].equals(BigInteger.ZERO)) {
                        result[iNzColumn] = ecc.subtract(result[iNzColumn], result[nonMaxLisColumn].multiply(lhs[iNzRow][nonMaxLisColumn]));
                    }
                }
            }
        }
        return Consistent;
    }
}
