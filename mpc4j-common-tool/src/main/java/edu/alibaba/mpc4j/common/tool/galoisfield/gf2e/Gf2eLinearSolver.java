package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.linear.LinearSolver;
import cc.redberry.rings.util.ArraysUtil;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

import static cc.redberry.rings.linear.LinearSolver.SystemInfo.*;

/**
 * 求解线性方程组Ax = b，其中矩阵A为有限域的元素，而x与b为椭圆曲线的元素。
 * <p>
 * 此线性求解器代码参考了Rings中的实现（参见cc.redberry.rings.linear.LinearSolver）。
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/7/6
 */
public class Gf2eLinearSolver {
    /**
     * GF2E接口
     */
    private final Gf2e gf2e;

    public Gf2eLinearSolver(Gf2e gf2e) {
        this.gf2e = gf2e;
    }

    /**
     * Gives the row echelon form of the linear system {@code lhs.x = rhs}.
     *
     * @param lhs                    the lhs of the system.
     * @param rhs                    the rhs of the system.
     * @param breakOnUnderDetermined whether to return immediately if it was detected that system is under determined.
     * @return the number of free variables.
     */
    private int rowEchelonForm(byte[][][] lhs, byte[][] rhs, boolean breakOnUnderDetermined) {
        if (rhs != null && lhs.length != rhs.length) {
            throw new IllegalArgumentException("lhs.length != rhs.length");
        }
        if (lhs.length == 0) {
            return 0;
        }
        int nRows = lhs.length;
        int nColumns = lhs[0].length;
        // number of zero columns
        int nZeroColumns = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            int row = iColumn - nZeroColumns;
            int max = row;
            if (gf2e.isZero(lhs[row][iColumn])) {
                for (int iRow = row + 1; iRow < nRows; ++iRow) {
                    if (!gf2e.isZero(lhs[iRow][iColumn])) {
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
            if (gf2e.isZero(lhs[row][iColumn])) {
                if (breakOnUnderDetermined) {
                    return 1;
                }
                // nothing to do on this column
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }
            // pivot within A and b
            for (int iRow = row + 1; iRow < nRows; ++iRow) {
                byte[] alpha = gf2e.div(lhs[iRow][iColumn], lhs[row][iColumn]);
                if (rhs != null) {
                    gf2e.subi(rhs[iRow], gf2e.mul(rhs[row], alpha));
                }
                if (!gf2e.isZero(alpha)) {
                    for (int iCol = iColumn; iCol < nColumns; ++iCol) {
                        gf2e.subi(lhs[iRow][iCol], gf2e.mul(alpha, lhs[row][iCol]));
                    }
                }
            }
        }
        return nZeroColumns;
    }

    /**
     * Solves linear system {@code lhs.x = rhs} and reduces the lhs to row echelon form. The result is stored in {@code
     * result} (which should be of the enough length). Here rhs and result are represented as ECPoint.
     *
     * @param lhs                    the lhs of the system (will be reduced to row echelon form).
     * @param rhs                    the rhs of the system.
     * @param result                 where to place the result.
     * @param solveIfUnderDetermined give some solution even if the system is under determined.
     * @return system information (inconsistent, under-determined or consistent).
     */
    public LinearSolver.SystemInfo solve(byte[][][] lhs, byte[][] rhs, byte[][] result, boolean solveIfUnderDetermined) {
        if (lhs.length != rhs.length) {
            throw new IllegalArgumentException("lhs.length != rhs.length");
        }
        if (rhs.length == 0) {
            return Consistent;
        }
        if (rhs.length == 1) {
            // 如果只有一个约束条件，则方程变为ax = b，此时x = b * (a^-1)
            if (lhs[0].length == 1) {
                result[0] = gf2e.div(rhs[0], lhs[0][0]);
                return Consistent;
            }
            if (solveIfUnderDetermined) {
                Arrays.fill(result, gf2e.createZero());
                // 如果b = 0，则方程组的形式全部为ax = 0，此时x = 0
                if (gf2e.isZero(rhs[0])) {
                    return Consistent;
                }
                // 如果b != 0，则只需要考虑第一个约束条件
                for (int i = 0; i < result.length; ++i) {
                    if (!gf2e.isZero(lhs[0][i])) {
                        result[i] = gf2e.div(rhs[0], lhs[0][i]);
                        return Consistent;
                    }
                }
                return Inconsistent;
            }
            if (lhs[0].length > 1) {
                return UnderDetermined;
            }
            return Inconsistent;
        }

        int nUnderDetermined = rowEchelonForm(lhs, rhs, !solveIfUnderDetermined);
        if (!solveIfUnderDetermined && nUnderDetermined > 0) {
            // under-determined system
            return UnderDetermined;
        }

        int nRows = rhs.length;
        int nColumns = lhs[0].length;

        if (!solveIfUnderDetermined && nColumns > nRows) {
            // under-determined system
            return UnderDetermined;
        }

        if (nRows > nColumns) {
            // over-determined system
            // check that all rhs are zero
            for (int i = nColumns; i < nRows; ++i) {
                if (!gf2e.isZero(rhs[i])) {
                    // inconsistent system
                    return Inconsistent;
                }
            }
        }

        if (nRows > nColumns) {
            for (int i = nColumns + 1; i < nRows; ++i) {
                if (!gf2e.isZero(rhs[i])) {
                    return Inconsistent;
                }
            }
        }

        Arrays.fill(result, gf2e.createZero());
        // back substitution in case of determined system
        if (nUnderDetermined == 0 && nColumns <= nRows) {
            for (int i = nColumns - 1; i >= 0; i--) {
                byte[] sum = gf2e.createZero();
                for (int j = i + 1; j < nColumns; j++) {
                    gf2e.addi(sum, gf2e.mul(result[j], lhs[i][j]));
                }
                result[i] = gf2e.sub(rhs[i], sum);
                gf2e.divi(result[i], lhs[i][i]);
            }
            return Consistent;
        }

        // back substitution in case of underdetermined system
        TIntArrayList nzColumns = new TIntArrayList(), nzRows = new TIntArrayList();
        //number of zero columns
        int nZeroColumns = 0;
        int iRow = 0;
        for (int iColumn = 0, to = Math.min(nRows, nColumns); iColumn < to; ++iColumn) {
            // find pivot row and swap
            iRow = iColumn - nZeroColumns;
            if (gf2e.isZero(lhs[iRow][iColumn])) {
                if (iColumn == (nColumns - 1) && !gf2e.isZero(rhs[iRow])) {
                    return Inconsistent;
                }
                ++nZeroColumns;
                to = Math.min(nRows + nZeroColumns, nColumns);
                continue;
            }

            // scale current row
            byte[][] row = lhs[iRow];
            // 后面row[iColumn]会做修改，这里需要拷贝一份
            byte[] val = BytesUtils.clone(row[iColumn]);
            byte[] valInv = gf2e.inv(val);

            for (int i = iColumn; i < nColumns; i++) {
                row[i] = gf2e.mul(valInv, row[i]);
            }

            gf2e.muli(rhs[iRow], valInv);

            // scale all rows before
            for (int i = 0; i < iRow; i++) {
                byte[][] pRow = lhs[i];
                // 后面pRow[iColumn]会做修改，这里需要拷贝一份
                byte[] v = BytesUtils.clone(pRow[iColumn]);
                if (gf2e.isZero(v)) {
                    continue;
                }
                for (int j = iColumn; j < nColumns; ++j) {
                    gf2e.subi(pRow[j], gf2e.mul(v, row[j]));
                }
                gf2e.subi(rhs[i], gf2e.mul(rhs[iRow], v));
            }
            if (!gf2e.isZero(rhs[iRow]) && gf2e.isZero(lhs[iRow][iColumn])) {
                return Inconsistent;
            }
            nzColumns.add(iColumn);
            nzRows.add(iRow);
        }

        ++iRow;
        if (iRow < nRows) {
            for (; iRow < nRows; ++iRow) {
                if (!gf2e.isZero(rhs[iRow])) {
                    return Inconsistent;
                }
            }
        }

        for (int i = 0; i < nzColumns.size(); ++i) {
            result[nzColumns.get(i)] = rhs[nzRows.get(i)];
        }

        return Consistent;
    }
}
