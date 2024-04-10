package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import cc.redberry.rings.util.ArraysUtil;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.Consistent;
import static cc.redberry.rings.linear.LinearSolver.SystemInfo.Inconsistent;

/**
 * Solving the linear equation Ax = b, where A is a matrix with n×m Zp64 elements, y is a vector with Zp64 elements.
 * <p>
 * Each row in A is a band vector, that is, for the row i, only A[i][s_i], A[i][s_i + 1], ..., A[i][s_i + w - 1]
 * is non-zero, where s_i ∈ [0, n - w).
 * </p>
 * Pictorially, the matrix A is like the following form:
 * <p>| - - - 0 1 1 0 0 - - - |</p>
 * <p>| 1 0 0 1 1 - - - - - - |</p>
 * <p>| - - - - - - 0 0 1 0 1 |</p>
 * <p>| - - - - 1 1 0 1 0 - - |</p>
 * where n = 4, m = 11, w = 5, s_0 = 1, s_1 = 0, s_2 = 6, s_3 = 4. The picture is from Figure 4 of the following paper:
 * <p>
 * Patel, Sarvar, Joon Young Seo, and Kevin Yeo. Don’t be Dense: Efficient Keyword PIR for Sparse Databases. To appear
 * in USENIX Security 2023.
 * </p>
 * The algorithm for solving the equation is described in Algorithm 1 of the following paper:
 * <p>
 * Dietzfelbinger, Martin, and Stefan Walzer. Efficient Gauss Elimination for Near-Quadratic Matrices with One Short
 * Random Block per Row, with Applications. ESA 2019.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/8/4
 */
public class Zp64BandLinearSolver {
    /**
     * Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public Zp64BandLinearSolver(Zp64 zp64) {
        this(zp64, new SecureRandom());
    }

    public Zp64BandLinearSolver(Zp64 zp64, SecureRandom secureRandom) {
        this.zp64 = zp64;
        this.secureRandom = secureRandom;
    }

    private static void sort(int[] ss, long[][] lhs, long[] rhs) {
        int nRows = lhs.length;
        // sort s[...] and get the permutation map
        List<Integer> permutationIndices = IntStream.range(0, ss.length).boxed().collect(Collectors.toList());
        Comparator<Integer> comparator = Comparator.comparingInt(i -> ss[i]);
        permutationIndices.sort(comparator);
        int[] permutationMap = permutationIndices.stream().mapToInt(i -> i).toArray();
        TIntIntMap map = new TIntIntHashMap(nRows);
        IntStream.range(0, nRows).forEach(permuteIndex -> {
            int index = permutationMap[permuteIndex];
            map.put(index, permuteIndex);
        });
        // permute ss, lhs and rhs based on the map
        int[] copySs = IntUtils.clone(ss);
        long[][] copyLhs = LongUtils.clone(lhs);
        long[] copyRhs = LongUtils.clone(rhs);
        for (int iRow = 0; iRow < nRows; iRow++) {
            int iPermuteRow = map.get(iRow);
            ss[iPermuteRow] = copySs[iRow];
            lhs[iPermuteRow] = copyLhs[iRow];
            rhs[iPermuteRow] = copyRhs[iRow];
        }
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs} for the band form ow lhs. Note that here we
     * only allow
     * <p> m (number of columns) >= n (number of rows) </p>
     *
     * @param ss       starting positions of lhs.
     * @param w        the band width.
     * @param lhs      the lhs of the system.
     * @param rhs      the rhs of the system.
     * @param nColumns the number of columns.
     * @return the information for row Echelon form.
     */
    private RowEchelonFormInfo rowEchelonForm(int w, int[] ss, long[][] lhs, long[] rhs, int nColumns) {
        // verification is done in the input phase
        int nRows = ss.length;
        TIntSet maxLisColumns = new TIntHashSet(nRows);
        // sort the rows of the system (A, b) by s[i]
        sort(ss, lhs, rhs);
        // number of zero columns, here we consider if some columns are 0.
        int nZeroColumns = 0;
        // for all possible columns
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            // if the current pivot is 0, then search for leftmost 1 in row i.
            if (zp64.isZero(getValue(w, ss[row], lhs[row], iColumn))) {
                // There can be many candidate rows. Since s_i is ordered, once we find an invalid row, we can break.
                for (int iRow = row + 1; iRow < nRows && iColumn >= ss[iRow] && iColumn < ss[iRow] + w; ++iRow) {
                    if (!zp64.isZero(getValue(w, ss[iRow], lhs[iRow], iColumn))) {
                        max = iRow;
                        break;
                    }
                }
                // We swap rows in the implementation. We change the starting position to ensure ss is correct.
                if (ss[row] < ss[max]) {
                    long[] temp = new long[lhs[row].length];
                    for (int i = 0; i < lhs[row].length - (ss[max] - ss[row]); i++) {
                        temp[i] = lhs[row][ss[max] - ss[row]];
                    }
                    lhs[row] = Arrays.copyOf(temp, lhs[row].length);
                    ss[row] = ss[max];
                }
                ArraysUtil.swap(ss, row, max);
                ArraysUtil.swap(lhs, row, max);
                ArraysUtil.swap(rhs, row, max);
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (zp64.isZero(getValue(w, ss[row], lhs[row], iColumn))) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination, since s_i is ordered, once we find an invalid row, we can break.
            for (int iRow = row + 1; iRow < nRows && iColumn >= ss[iRow] && iColumn < ss[iRow] + w; ++iRow) {
                long alpha = zp64.div(getValue(w, ss[iRow], lhs[iRow], iColumn), getValue(w, ss[row], lhs[row], iColumn));
                rhs[iRow] = zp64.sub(rhs[iRow], zp64.mul(rhs[row], alpha));
                if (!zp64.isZero(alpha)) {
                    for (int iCol = iColumn; iCol >= ss[iRow] && iCol < ss[iRow] + w; ++iCol) {
                        long temp = zp64.sub(
                            getValue(w, ss[iRow], lhs[iRow], iCol), zp64.mul(alpha, getValue(w, ss[row], lhs[row], iCol))
                        );
                        lhs[iRow][iCol - ss[iRow]] = temp;
                    }
                }
            }
        }
        // we loop for all possible columns, each inner loop may be large, but only around w.
        return new RowEchelonFormInfo(nZeroColumns, maxLisColumns);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as zero. Note that each row in lhs is a band
     * vector, and lsh is in is modified when solving the system.
     *
     * @param ss       the starting positions for rows.
     * @param nColumns number of columns.
     * @param lhBands  the lhs of the system in band form (will be reduced to row echelon form).
     * @param rhs      the rhs of the system.
     * @param result   where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo freeSolve(int[] ss, int nColumns, long[][] lhBands,
                                long[] rhs, long[] result) {
        return solve(ss, nColumns, lhBands, rhs, result, false);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as random. Note that lsh is modified when
     * solving the system.
     *
     * @param ss       the starting positions for rows.
     * @param nColumns number of columns.
     * @param lhBands  the lhs of the system in band form (will be reduced to row echelon form).
     * @param rhs    the rhs of the system.
     * @param result where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo fullSolve(int[] ss, int nColumns, long[][] lhBands,
                                long[] rhs, long[] result) {
        return solve(ss, nColumns, lhBands, rhs, result, true);
    }

    private SystemInfo solve(int[] ss, int nColumns, long[][] lhBands, long[] rhs,
                             long[] result, boolean isFull) {
        MathPreconditions.checkNonNegative("n", rhs.length);
        int nRows = rhs.length;
        // ss.length == rows of rhs
        MathPreconditions.checkEqual("ss.length", "nRows", ss.length, nRows);
        // rows of lhs == n
        MathPreconditions.checkEqual("lhBands.length", "n", lhBands.length, nRows);
        // m >= n
        MathPreconditions.checkGreaterOrEqual("m", nColumns, nRows);
        // result.length == m
        MathPreconditions.checkEqual("result.length", "m", result.length, nColumns);
        if (nRows == 0) {
            // if n = 0, all solutions are good.
            if (isFull) {
                // full random variables
                for (int iColumn = 0; iColumn < nColumns; iColumn++) {
                    result[iColumn] = zp64.createNonZeroRandom(secureRandom);
                }
            } else {
                // full zero variables
                Arrays.fill(result, zp64.createZero());
            }
            return Consistent;
        }
        // 0 < w <= m, we allow w = m
        MathPreconditions.checkPositiveInRangeClosed("w", lhBands[0].length, nColumns);
        int w = lhBands[0].length;
        // each bandwidth is w
        Arrays.stream(lhBands).forEach(row -> MathPreconditions.checkEqual("row.length", "w", row.length, w));
        // 0 <= s_i <= m - w
        Arrays.stream(ss).forEach(si -> MathPreconditions.checkNonNegativeInRangeClosed("s[i]", si, nColumns - w));
        // treat the band as A, the initial value is 0
        if (nRows == 1) {
            return solveOneRow(w, ss[0], lhBands[0], rhs[0], result, isFull);
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFormInfo info = rowEchelonForm(w, ss, lhBands, rhs, nColumns);
        int nUnderDetermined = info.getZeroColumnNum();
        Arrays.fill(result, zp64.createZero());
        // for determined system, free and full solution are the same
        if (nUnderDetermined == 0 && nColumns == nRows) {
            for (int i = nRows - 1; i >= 0; i--) {
                long sum = 0L;
                for (int j = i + 1; j < ss[i] + w; j++) {
                    sum = zp64.add(sum, zp64.mul(result[j], getValue(w, ss[i], lhBands[i], j)));
                }
                result[i] = zp64.div(zp64.sub(rhs[i], sum), getValue(w, ss[i], lhBands[i], i));
            }
            return Consistent;
        }
        return solveUnderDeterminedRows(w, ss, lhBands, rhs, result, info, isFull);
    }

    private SystemInfo solveOneRow(int w, int s0, long[] lh0, long rh0, long[] result, boolean isFull) {
        int nColumns = result.length;
        // when n = 1, then the linear system only has one equation a[0]x[0] + ... + a[m]x[m] = b[0]
        if (nColumns == 1) {
            // if m = 1, then we directly compute a[0]x[0] = b[0]
            if (!zp64.isZero(lh0[0])) {
                // a[0] != 0, x[0] = b[0] / a[0]
                result[0] = zp64.div(rh0, lh0[0]);
                return Consistent;
            } else {
                // a[0] == 0, it can be solved only if b[0] = 0
                if (zp64.isZero(rh0)) {
                    result[0] = isFull ? zp64.createNonZeroRandom(secureRandom) : zp64.createZero();
                    return Consistent;
                } else {
                    return Inconsistent;
                }
            }
        }
        // if m > 1, the linear system a[0]x[0] + ... + a[m]x[m] = b[0] contains free variables.
        Arrays.fill(result, zp64.createZero());
        // find the first non-zero a[t]
        int firstNonZeroColumn = -1;
        for (int iColumn = s0; iColumn >= s0 && iColumn < s0 + w; ++iColumn) {
            if (!zp64.isZero(getValue(w, s0, lh0, iColumn))) {
                firstNonZeroColumn = iColumn;
                break;
            }
        }
        // if all a[i] = 0, we have solution only if b[0] = 0
        if (firstNonZeroColumn == -1) {
            if (zp64.isZero(rh0)) {
                if (isFull) {
                    // full random variables
                    for (int i = 0; i < nColumns; i++) {
                        result[i] = zp64.createNonZeroRandom(secureRandom);
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
                    result[i] = zp64.createNonZeroRandom(secureRandom);
                    if (!zp64.isZero(getValue(w, s0, lh0, i))) {
                        // a[i] != 0, b[0] = b[0] - a[i] * x[i].
                        rh0 = zp64.sub(rh0, zp64.mul(getValue(w, s0, lh0, i), result[i]));
                    }
                }
            }
            // set x[t] = b[0] / a[0]
            result[firstNonZeroColumn] = zp64.div(rh0, getValue(w, s0, lh0, firstNonZeroColumn));
            return Consistent;
        }
    }

    private SystemInfo solveUnderDeterminedRows(int w, int[] ss, long[][] lhs, long[] rhs,
                                                long[] result, RowEchelonFormInfo info, boolean isFull) {
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
            if (zp64.isZero(getValue(w, ss[iRow], lhs[iRow], iColumn))) {
                if (iColumn == (nColumns - 1) && !zp64.isZero(rhs[iRow])) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                // full solution needs to set the corresponding result[iColumn] as a random variable
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // scale current row, that is, make lhs[iRow][iColumn] = 1, and scale other entries in this row.
            long val = getValue(w, ss[iRow], lhs[iRow], iColumn);
            long valInv = zp64.inv(val);
            for (int i = iColumn; i < ss[iRow] + w; i++) {
                lhs[iRow][i - ss[iRow]] = zp64.mul(valInv, getValue(w, ss[iRow], lhs[iRow], i));
            }
            rhs[iRow] = zp64.mul(rhs[iRow], valInv);
            // here we cannot scale all rows before, otherwise the procedure is O(n^2). For example:
            // | 1 1 0 0 0 0 0 |                   | 1 1 0 1 0 0 0 |
            // | 0 1 1 0 0 0 0 | (reduce last row) | 0 1 1 1 0 0 0 | The first row is no longer a band vector.
            // | 0 0 1 1 0 0 0 |                   | 0 0 1 0 0 0 0 |
            // | 0 0 0 1 0 0 0 |                   | 0 0 0 1 0 0 0 |
            if (!zp64.isZero(rhs[iRow]) && zp64.isZero(getValue(w, ss[iRow], lhs[iRow], iColumn))) {
                return Inconsistent;
            }
            // label that column and its corresponding row for the solution b[row].
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }
        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!zp64.isZero(rhs[iRow])) {
                    return Inconsistent;
                }
            }
        }
        // we need to solve equations using back substitution
        if (isFull) {
            // full non-maxLisColumns first
            TIntSet maxLisColumns = info.getMaxLisColumns();
            TIntSet nonMaxLisColumns = new TIntHashSet(nColumns);
            nonMaxLisColumns.addAll(IntStream.range(0, nColumns).toArray());
            nonMaxLisColumns.removeAll(maxLisColumns);
            int[] nonMaxLisColumnArray = nonMaxLisColumns.toArray();
            // set result[iColumn] corresponding to the non-maxLisColumns as random variables
            for (int nonMaxLisColumn : nonMaxLisColumnArray) {
                result[nonMaxLisColumn] = zp64.createNonZeroRandom(secureRandom);
            }
        }
        for (int i = nzColumns.size() - 1; i >= 0; i--) {
            int iResultColumn = nzColumns.get(i);
            int iResultRow = nzRows.get(i);
            long tempResult = rhs[iResultRow];
            for (int j = ss[iResultRow]; j < ss[iResultRow] + w; j++) {
                tempResult = zp64.sub(tempResult, zp64.mul(getValue(w, ss[iResultRow], lhs[iResultRow], j), result[j]));
            }
            result[iResultColumn] = tempResult;
        }
        return Consistent;
    }

    /**
     * Returns the zp64 element in long array with given index and offset.
     *
     * @param w     the band width.
     * @param s0    starting positions of lhs.
     * @param array the byte array.
     * @param index the index.
     * @return the zp64 element.
     */
    private long getValue(int w, int s0, long[] array, int index) {
        if (index >= (w + s0) || index < s0) {
            return 0;
        } else {
            return array[index - s0];
        }
    }
}
