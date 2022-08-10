package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.MaxLisFinder;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Z_p域最大线性无关组（Maximal Linearly Independent System）查找器。实现参考下述高斯消元法实现。
 * <p>
 * https://algs4.cs.princeton.edu/99scientific/GaussianElimination.java.html
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/09/11
 */
public class ZpMaxLisFinder implements MaxLisFinder {
    /**
     * Z_p域的质数p
     */
    private final BigInteger prime;
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
    private final BigInteger[][] copiedMatrix;
    /**
     * 行号标识
     */
    private final int[] rowLabels;

    public ZpMaxLisFinder(BigInteger prime, BigInteger[][] matrix) {
        // 验证p是否为质数
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH);
        this.prime = prime;
        // 设置矩阵行数和列数，并检查输入矩阵的有效性
        m = matrix.length;
        assert m > 0;
        n = matrix[0].length;
        assert n > 0 && m >= n;
        for (int rowIndex = 0; rowIndex < m; rowIndex++) {
            assert matrix[rowIndex].length == n;
        }
        // 复制一份矩阵，因为查找线性无关组时需要在矩阵上操作
        copiedMatrix = new BigInteger[m][n];
        IntStream.range(0, m)
            .forEach(rowIndex -> IntStream.range(0, n)
                .forEach(columnIndex -> copiedMatrix[rowIndex][columnIndex] = matrix[rowIndex][columnIndex].mod(prime))
            );
        rowLabels = IntStream.range(0, m).toArray();
        forwardElimination();
    }

    private void forwardElimination() {
        for (int p = 0; p < n; p++) {
            // find pivot row using partial pivoting
            int max = p;
            for (int i = p + 1; i < m; i++) {
                if (copiedMatrix[i][p].compareTo(copiedMatrix[max][p]) > 0) {
                    max = i;
                }
            }
            // swap
            swap(p, max);
            // 奇异或近似奇异，需要继续往后计算
            if (copiedMatrix[p][p].compareTo(BigInteger.ZERO) == 0) {
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
        BigInteger[] tempRow = copiedMatrix[row1];
        copiedMatrix[row1] = copiedMatrix[row2];
        copiedMatrix[row2] = tempRow;
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
            BigInteger alpha = copiedMatrix[i][p].multiply(copiedMatrix[p][p].modInverse(prime)).mod(prime);
            for (int j = p; j < n; j++) {
                copiedMatrix[i][j] = copiedMatrix[i][j].subtract(alpha.multiply(copiedMatrix[p][j])).mod(prime);
            }
        }
    }

    @Override
    public Set<Integer> getLisRows() {
        Set<Integer> lisRowSet = new HashSet<>(n);
        for (int p = 0; p < m; p++) {
            boolean isLinearIndependent = false;
            for (int j = p; j < n; j++) {
                // 只要有1个不为0，此行就是线性无关行
                if (!copiedMatrix[p][j].equals(BigInteger.ZERO)) {
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
