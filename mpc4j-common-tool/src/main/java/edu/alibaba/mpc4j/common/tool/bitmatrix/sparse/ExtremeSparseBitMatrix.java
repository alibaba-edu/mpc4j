package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

/**
 * Extremely Sparse Matrix 类
 * SparseMatrix 类考虑了各行或者各列仅有少数点为1的情况
 * Extremely Sparse Matrix 类针对大部分行或者列为空的情况做了优化，节省了乘法运算的循环开销。
 * 目前仅支持按列存储，支持左乘运算
 *
 * @author Hanwen Feng
 * @date 2022/03/10
 */
public class ExtremeSparseBitMatrix {
    /**
     * 记录非空列的index
     */
    private final int[] nonEmptyColIndex;
    /**
     * 存储非空列的list
     */
    private final ArrayList<SparseBitVector> colsList;
    /**
     * 矩阵的列数
     */
    private final int cols;
    /**
     * 矩阵的行数
     */
    private final int rows;

    /**
     * 构造函数
     *
     * @param colsList         存储非空列的list
     * @param nonEmptyColIndex 记录非空列index的数组
     * @param rows             行数
     * @param cols             列数
     */
    public ExtremeSparseBitMatrix(ArrayList<SparseBitVector> colsList, int[] nonEmptyColIndex, int rows, int cols) {
        this.colsList = colsList;
        this.nonEmptyColIndex = nonEmptyColIndex;
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * 布尔向量左乘矩阵， e*M
     *
     * @param xVec 布尔向量
     * @return 返回乘积
     */
    public boolean[] lmul(boolean[] xVec) {
        assert xVec.length == rows;
        boolean[] outputs = new boolean[cols];
        for (int i = 0; i < nonEmptyColIndex.length; i++) {
            int index = nonEmptyColIndex[i];
            outputs[index] = colsList.get(i).multiply(xVec);
        }
        return outputs;
    }

    /**
     * 计算布尔向量左乘矩阵再和布尔向量相加 x*M+y，结果返回到y
     *
     * @param xVec 向量
     * @param yVec 向量
     */
    public void lmulAddi(boolean[] xVec, boolean[] yVec) {
        assert xVec.length == rows;
        assert yVec.length == cols;
        for (int i = 0; i < nonEmptyColIndex.length; i++) {
            int index = nonEmptyColIndex[i];
            yVec[index] ^= colsList.get(i).multiply(xVec);
        }

    }

    /**
     * byte[][]向量左乘矩阵， x*M
     *
     * @param xVec 向量
     * @return 返回乘积
     */
    public byte[][] lExtMul(byte[][] xVec) {
        assert xVec.length == rows;

        byte[][] outputs = new byte[cols][xVec[0].length];
        for (int i = 0; i < nonEmptyColIndex.length; i++) {
            int index = nonEmptyColIndex[i];
            colsList.get(i).multiplyAddi(xVec, outputs[index]);
        }
        return outputs;
    }

    /**
     * 计算byte[][]向量左乘矩阵再和布尔向量相加 x*M+y，结果返回到y
     *
     * @param xVec 向量
     * @param yVec 向量
     */
    public void lExtMulAddi(byte[][] xVec, byte[][] yVec) {
        assert xVec.length == rows;
        assert yVec.length == cols;
        assert xVec[0].length == yVec[0].length;

        for (int i = 0; i < nonEmptyColIndex.length; i++) {
            int index = nonEmptyColIndex[i];
            colsList.get(i).multiplyAddi(xVec, yVec[index]);
        }
    }

    /**
     * 读取矩阵列数
     *
     * @return 列数
     */
    public int getCols() {
        return cols;
    }

    /**
     * 读取矩阵行数
     *
     * @return 行数
     */
    public int getRows() {
        return rows;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExtremeSparseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ExtremeSparseBitMatrix that = (ExtremeSparseBitMatrix) obj;
        return new EqualsBuilder().append(this.colsList, that.colsList).append(this.rows, that.rows)
            .append(this.cols, that.cols).append(this.nonEmptyColIndex, that.nonEmptyColIndex).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(colsList).
            append(rows).append(cols).append(nonEmptyColIndex).toHashCode();
    }

}
