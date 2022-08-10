package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.MaxLisFinder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 二进制矩阵最大线性无关向量查找器。
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
public class Gf2eMaxLisFinder implements MaxLisFinder {
    /**
     * GF2E
     */
    private final Gf2e gf2e;
    /**
     * 行数
     */
    private final int m;
    /**
     * 列数
     */
    private final int n;
    /**
     * 拷贝的矩阵
     */
    private final byte[][][] copiedMatrix;
    /**
     * 行号标识
     */
    private final int[] rowLabels;

    public Gf2eMaxLisFinder(Gf2e gf2e, byte[][][] matrix) {
        this.gf2e = gf2e;
        // 设置矩阵行数和列数，并检查输入矩阵的有效性
        m = matrix.length;
        assert m > 0;
        n = matrix[0].length;
        assert n > 0 && m >= n;
        for (int rowIndex = 0; rowIndex < m; rowIndex++) {
            assert matrix[rowIndex].length == n;
        }
        // 复制一份矩阵，因为查找线性无关组时需要在矩阵上操作
        copiedMatrix = new byte[m][n][];
        IntStream.range(0, m)
            .forEach(rowIndex -> IntStream.range(0, n)
                .forEach(columnIndex -> {
                    if (gf2e.isOne(matrix[rowIndex][columnIndex])) {
                        copiedMatrix[rowIndex][columnIndex] = gf2e.createOne();
                    } else if (gf2e.isZero(matrix[rowIndex][columnIndex])) {
                        copiedMatrix[rowIndex][columnIndex] = gf2e.createZero();
                    } else {
                        throw new IllegalArgumentException(String.format(
                            "row = %s, column = %s is not 0 or 1", rowIndex, columnIndex
                        ));
                    }
                })
            );
        rowLabels = IntStream.range(0, m).toArray();
        forwardElimination();
    }

    private void forwardElimination() {
        for (int p = 0; p < n; p++) {
            // find pivot row using partial pivoting
            int max = p;
            for (int i = p + 1; i < m; i++) {
                if (!gf2e.isZero(copiedMatrix[i][p])) {
                    max = i;
                }
            }
            // swap
            swap(p, max);
            // 奇异或近似奇异，需要继续往后计算
            if (gf2e.isZero(copiedMatrix[p][p])) {
                continue;
            }
            // pivot
            pivot(p);
        }
    }

    /**
     * swap row1 and row2.
     *
     * @param row1 row1.
     * @param row2 row2.
     */
    private void swap(int row1, int row2) {
        // 交换矩阵内容
        byte[][] rowTemp = copiedMatrix[row1];
        copiedMatrix[row1] = copiedMatrix[row2];
        copiedMatrix[row2] = rowTemp;
        // 交换行号
        int rowIndexTemp = rowLabels[row1];
        rowLabels[row1] = rowLabels[row2];
        rowLabels[row2] = rowIndexTemp;
    }

    /**
     * pivot on a[p][p].
     *
     * @param p p.
     */
    private void pivot(int p) {
        for (int i = p + 1; i < m; i++) {
            byte[] alpha = gf2e.div(copiedMatrix[i][p], copiedMatrix[p][p]);
            for (int j = p; j < n; j++) {
                copiedMatrix[i][j] = gf2e.sub(copiedMatrix[i][j], gf2e.mul(alpha, copiedMatrix[p][j]));
            }
        }
    }

    @Override
    public Set<Integer> getLisRows() {
        Set<Integer> lisRowSet = new HashSet<>(n);
        for (int p = 0; p < m; p++) {
            boolean isLinearIndependent = false;
            for (int j = p; j < n; j++) {
                if (!gf2e.isZero(copiedMatrix[p][j])) {
                    isLinearIndependent = true;
                    break;
                }
            }
            if (isLinearIndependent) {
                lisRowSet.add(rowLabels[p]);
            }
        }
        return lisRowSet;
    }
}
