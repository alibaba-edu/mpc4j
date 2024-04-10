package edu.alibaba.mpc4j.common.structure.okve.tool;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import cc.redberry.rings.util.ArraysUtil;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;
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
 * binary band linear solver.
 *
 * @author Weiran Liu
 * @date 2023/8/4
 */
public class BinaryBandLinearSolver {
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

    public BinaryBandLinearSolver(int l) {
        this(l, new SecureRandom());
    }

    public BinaryBandLinearSolver(int l, SecureRandom secureRandom) {
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        zeroElement = new byte[byteL];
        Arrays.fill(zeroElement, (byte) 0x00);
        this.secureRandom = secureRandom;
    }

    private static void sort(int[] ss, byte[][] lhs, byte[][] rhs) {
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
        byte[][] copyLhs = BytesUtils.clone(lhs);
        byte[][] copyRhs = BytesUtils.clone(rhs);
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
     * @param ss  starting positions of lhs.
     * @param w   the band width.
     * @param lhs the lhs of the system.
     * @param rhs the rhs of the system.
     * @return the information for row Echelon form.
     */
    private RowEchelonFormInfo rowEchelonForm(int w, int nColumns, int[] ss, byte[][] lhs, byte[][] rhs) {
        // verification is done in the input phase
        int nRows = ss.length;
        int wByteColumns = CommonUtils.getByteLength(w);
        int wOffsetColumns = wByteColumns * Byte.SIZE - w;
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
            if (!getBooleanValue(w, ss[row], lhs[row], iColumn, wOffsetColumns)) {
                // There can be many candidate rows. Since s_i is ordered, once we find an invalid row, we can break.
                for (int iRow = row + 1; iRow < nRows && iColumn >= ss[iRow] && iColumn < ss[iRow] + w; ++iRow) {
                    if (getBooleanValue(w, ss[iRow], lhs[iRow], iColumn, wOffsetColumns)) {
                        max = iRow;
                        break;
                    }
                }
                // We swap rows in the implementation. We change the starting position to ensure ss is correct.
                if (ss[row] < ss[max]) {
                    BytesUtils.shiftLefti(lhs[row], ss[max] - ss[row]);
                    ss[row] = ss[max];
                }
                ArraysUtil.swap(ss, row, max);
                ArraysUtil.swap(lhs, row, max);
                ArraysUtil.swap(rhs, row, max);
            }
            // if we cannot find one, it means this column is free, nothing to do on this column
            if (!getBooleanValue(w, ss[row], lhs[row], iColumn, wOffsetColumns)) {
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // add that column into the set
            maxLisColumns.add(iColumn);
            // forward Gaussian elimination, since s_i is ordered, once we find an invalid row, we can break.
            for (int iRow = row + 1; iRow < nRows && iColumn >= ss[iRow] && iColumn < ss[iRow] + w; ++iRow) {
                boolean alpha = getBooleanValue(w, ss[iRow], lhs[iRow], iColumn, wOffsetColumns);
                if (alpha) {
                    BytesUtils.xori(rhs[iRow], rhs[row]);
                    byte[] temp = BytesUtils.shiftLeft(lhs[row], ss[iRow] - ss[row]);
                    BytesUtils.xori(lhs[iRow], temp);
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
     * @param w        bandwidth.
     * @param lhBands  the lhs of the system in band form (will be reduced to row echelon form).
     * @param rhs      the rhs of the system.
     * @param result   where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo freeSolve(int[] ss, int nColumns, int w, byte[][] lhBands,
                                byte[][] rhs, byte[][] result) {
        return solve(ss, nColumns, w, lhBands, rhs, result, false);
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should have enough length). Free variables are set as random. Note that lsh is modified when
     * solving the system.
     *
     * @param ss       the starting positions for rows.
     * @param nColumns number of columns.
     * @param w        bandwidth.
     * @param lhBands  the lhs of the system in band form (will be reduced to row echelon form).
     * @param rhs      the rhs of the system.
     * @param result   where to place the result.
     * @return system information (inconsistent or consistent).
     */
    public SystemInfo fullSolve(int[] ss, int nColumns, int w, byte[][] lhBands,
                                byte[][] rhs, byte[][] result) {
        return solve(ss, nColumns, w, lhBands, rhs, result, true);
    }

    private SystemInfo solve(int[] ss, int nColumns, int w, byte[][] lhBands, byte[][] rhs,
                             byte[][] result, boolean isFull) {
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
                    result[iColumn] = createNonZeroRandom();
                }
            } else {
                // full zero variables
                Arrays.fill(result, createZero());
            }
            return Consistent;
        }
        // 0 < w <= m, we allow w = m
        MathPreconditions.checkPositiveInRangeClosed("w", w, nColumns);
        int byteW = CommonUtils.getByteLength(w);
        // each bandwidth is w
        Arrays.stream(lhBands).forEach(row ->
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, byteW, w))
        );
        // 0 <= s_i <= m - w
        Arrays.stream(ss).forEach(si -> MathPreconditions.checkNonNegativeInRangeClosed("s[i]", si, nColumns - w));
        // create A based on the band
        if (nRows == 1) {
            return solveOneRow(w, ss[0], lhBands[0], rhs[0], result, isFull);
        }
        // if n > 1, transform lsh to Echelon form.
        RowEchelonFormInfo info = rowEchelonForm(w, nColumns, ss, lhBands, rhs);
        int nUnderDetermined = info.getZeroColumnNum();
        Arrays.fill(result, createZero());
        int wByteColumns = CommonUtils.getByteLength(w);
        int wOffsetColumns = wByteColumns * Byte.SIZE - w;
        // for determined system, free and full solution are the same
        if (nUnderDetermined == 0 && nColumns == nRows) {
            for (int i = nColumns - 1; i >= 0; i--) {
                byte[] sum = createZero();
                for (int j = i + 1; j < ss[i] + w; j++) {
                    if (getBooleanValue(w, ss[i], lhBands[i], j, wOffsetColumns)) {
                        addi(sum, result[j]);
                    }
                }
                result[i] = sub(rhs[i], sum);
            }
            return Consistent;
        }
        return solveUnderDeterminedRows(w, ss, lhBands, rhs, result, info, isFull);
    }

    private SystemInfo solveOneRow(int w, int s0, byte[] lh0, byte[] rh0,
                                                byte[][] result, boolean isFull) {
        int nColumns = result.length;
        int nByteColumns = CommonUtils.getByteLength(nColumns);
        int nOffsetColumns = nByteColumns * Byte.SIZE - nColumns;
        int wByteColumns = CommonUtils.getByteLength(w);
        int wOffsetColumns = wByteColumns * Byte.SIZE - w;
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
        for (int iColumn = s0; iColumn >= s0 && iColumn < s0 + w; ++iColumn) {
            if (getBooleanValue(w, s0, lh0, iColumn, wOffsetColumns)) {
                firstNonZeroColumn = iColumn;
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
                    if (getBooleanValue(w, s0, lh0, i, wOffsetColumns)) {
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

    private SystemInfo solveUnderDeterminedRows(int w, int[] ss, byte[][] lhs, byte[][] rhs,
                                                byte[][] result, RowEchelonFormInfo info, boolean isFull) {
        int nRows = lhs.length;
        int nColumns = result.length;
        int wByteColumns = CommonUtils.getByteLength(w);
        int wOffsetColumns = wByteColumns * Byte.SIZE - w;
        // back substitution in case of under-determined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        // number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            iRow = iColumn - nZeroColumns;
            if (!getBooleanValue(w, ss[iRow], lhs[iRow], iColumn, wOffsetColumns)) {
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
            // here we cannot scale all rows before, otherwise the procedure is O(n^2). For example:
            // | 1 1 0 0 0 0 0 |                   | 1 1 0 1 0 0 0 |
            // | 0 1 1 0 0 0 0 | (reduce last row) | 0 1 1 1 0 0 0 | The first row is no longer a band vector.
            // | 0 0 1 1 0 0 0 |                   | 0 0 1 0 0 0 0 |
            // | 0 0 0 1 0 0 0 |                   | 0 0 0 1 0 0 0 |
            if (!isZero(rhs[iRow]) && !getBooleanValue(w, ss[iRow], lhs[iRow], iColumn, wOffsetColumns)) {
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
                result[nonMaxLisColumn] = createNonZeroRandom();
            }
        }
        for (int i = nzColumns.size() - 1; i >= 0; i--) {
            int iResultColumn = nzColumns.get(i);
            int iResultRow = nzRows.get(i);
            byte[] tempResult = BytesUtils.clone(rhs[iResultRow]);
            for (int j = ss[iResultRow]; j < ss[iResultRow] + w; j++) {
                if (getBooleanValue(w, ss[iResultRow], lhs[iResultRow], j, wOffsetColumns)) {
                    subi(tempResult, result[j]);
                }
            }
            result[iResultColumn] = tempResult;
        }
        return Consistent;
    }

    /**
     * Whether the element is zero.
     *
     * @param element the element.
     * @return true if the element is zero, otherwise false.
     */
    private boolean isZero(byte[] element) {
        return Arrays.equals(zeroElement, element);
    }

    /**
     * Creates zero element.
     *
     * @return zero.
     */
    private byte[] createZero() {
        return new byte[byteL];
    }

    /**
     * Creates non zero random element.
     *
     * @return non zero random element.
     */
    private byte[] createNonZeroRandom() {
        byte[] element;
        do {
            element = BytesUtils.randomByteArray(byteL, l, secureRandom);
        } while (isZero(element));
        return element;
    }

    /**
     * Adds the element q to p.
     *
     * @param p the element to overwrite.
     * @param q the other element.
     */
    private void addi(byte[] p, byte[] q) {
        BytesUtils.xori(p, q);
    }

    /**
     * Subtracts the element q from p.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p minus q.
     */
    private byte[] sub(byte[] p, byte[] q) {
        return BytesUtils.xor(p, q);
    }

    /**
     * Subtracts the element q from p.
     *
     * @param p the element p and to overwrite.
     * @param q the element q.
     */
    private void subi(byte[] p, byte[] q) {
        BytesUtils.xori(p, q);
    }

    /**
     * Returns the boolean value in byte array with given index and offset.
     *
     * @param w      the band width.
     * @param s0     starting positions of lhs.
     * @param array  the byte array.
     * @param index  the index.
     * @param offset the offset.
     * @return the boolean value.
     */
    private boolean getBooleanValue(int w, int s0, byte[] array, int index, int offset) {
        if (index < s0 || index >= w + s0) {
            return false;
        } else {
            return BinaryUtils.getBoolean(array, offset + index - s0);
        }
    }
}
