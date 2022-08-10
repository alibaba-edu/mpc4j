package edu.alibaba.mpc4j.common.tool.lpn.matrix;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * 循环矩阵类，仅指定首个列向量和循环周期即可定义矩阵
 * 以支持乘法操作。相比于SparseMatrix类大幅度节省内存。
 * 单线程实测性能接近，略高于SparseMatrix类,但是不容易支持多线程。
 * Ldpc Encoder的最终设计不采用
 *
 * @author Hanwen Feng
 * @date 2022.3.10
 */
public class CyclicSparseMatrix {
    /**
     * 定义循环矩阵的初始稀疏向量
     */
    private final int[] initColVector;
    /**
     * 循环的维度。例如，若 [1,2,5]循环维度是6，则循环移位结果为[0,2,3]；若循环维度是7,则循环移位结果是[2,3,6]
     */
    private final int cyclicLength;
    /**
     * 矩阵的行数
     */
    private final int rows;
    /**
     * 矩阵的列数
     */
    private final int cols;

    /**
     * 创建初始向量作为首个列向量 循环移位产生的矩阵
     * @param initColVector 初始向量
     * @param cyclicLength 循环维度
     * @param rows 矩阵行数
     * @param cols 矩阵列数
     */
    public CyclicSparseMatrix(int [] initColVector, int cyclicLength, int rows, int cols) {
        if (! isRegular(initColVector,cyclicLength, rows)) {
            throw  new IllegalArgumentException("The input is not regular");
        }
        this.cyclicLength = cyclicLength;
        this.rows = rows;
        this.initColVector = initColVector;
        this.cols = cols;
    }

    /**
     * 判断初始向量是否regular，即向量任意两个相邻的元素的差都大于 cyclicLength -rows。这意味着每一次移位最多有一个元素不属于该矩阵
     * @param initVector 初始向量
     * @param cyclicLength 循环维度
     * @param rows 行数
     * @return 判断是否regular
     */
    public static boolean isRegular(int[] initVector, int cyclicLength, int rows) {
        int minIntervalLength = cyclicLength - rows;
        for (int i = 0; i < initVector.length-1; i++) {
            int intervalLength = initVector[i+1] - initVector[i];
            if (intervalLength <= minIntervalLength) {
                return false;
            }
        }
        return initVector[0] - initVector[initVector.length - 1] + cyclicLength > minIntervalLength;
    }

    /**
     * 矩阵M左乘向量x， x*M
     * @param xVec 向量
     * @return 乘积
     */
    public boolean[] lmul(boolean[] xVec) {
        boolean[] outputs = new boolean[cols];
        lmulAdd(xVec,outputs);
        return outputs;
    }

    /**
     * 计算 x*M + y，并把结果返回到y
     * @param xVec 向量x
     * @param yVec 向量y
     */
    public void lmulAdd(boolean[] xVec, boolean[] yVec) {
        assert xVec.length == rows;
        assert yVec.length == cols;

        int [] currentCol = Arrays.copyOf(initColVector,initColVector.length);

        for (int i = 0; i< cols; i++) {
            int lastIndex = currentCol[currentCol.length-1];
            if (lastIndex < rows) {
                for (int j = 0; j < currentCol.length; j++) {
                    yVec[i] ^= xVec[currentCol[j]++];
                }
            } else if (lastIndex < cyclicLength-1) {
                for (int j = 0; j < currentCol.length -1; j++) {
                    yVec[i] ^= xVec[currentCol[j]++];
                }
                currentCol[currentCol.length -1]++;
            } else {
                currentCol[currentCol.length -1] = currentCol[currentCol.length-2] + 1;
                for (int j = currentCol.length -2; j >= 0; j--) {
                    yVec[i] ^= xVec[currentCol[j]];
                    currentCol[j] = (j == 0)? 0 : currentCol[j-1]+1;
                }
            }
        }
    }

    /**
     * 矩阵M左乘向量x， x*M
     * @param xVec 向量
     * @return 乘积
     */
    public byte[][] lmul(byte[][] xVec) {
        byte[][] outputs = new byte[cols][xVec[0].length];
        lmulAdd(xVec,outputs);
        return outputs;
    }

    /**
     * 计算 x*M + y，并把结果返回到y
     * @param xVec 向量x
     * @param yVec 向量y
     */
    public void lmulAdd(byte[][] xVec, byte[][] yVec) {
        assert xVec.length == rows;
        assert yVec.length == cols;
        assert xVec[0].length == yVec[0].length;
        int [] currentCol = Arrays.copyOf(initColVector,initColVector.length);


        for (int i = 0; i< cols; i++) {
            int lastIndex = currentCol[currentCol.length-1];
            if (lastIndex < rows) {
                for (int j = 0; j < currentCol.length; j++) {
                    BytesUtils.xori(yVec[i],xVec[currentCol[j]++]);
                }
            } else if (lastIndex < cyclicLength-1) {
                for (int j = 0; j < currentCol.length -1; j++) {
                    BytesUtils.xori(yVec[i],xVec[currentCol[j]++]);
                }
                currentCol[currentCol.length -1]++;
            } else {
                currentCol[currentCol.length -1] = currentCol[currentCol.length-2] + 1;
                for (int j = currentCol.length -2; j >= 0; j--) {
                    BytesUtils.xori(yVec[i],xVec[currentCol[j]]);
                    currentCol[j] = (j == 0)? 0 : currentCol[j-1]+1;
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CyclicSparseMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        CyclicSparseMatrix that = (CyclicSparseMatrix) obj;
        return new EqualsBuilder().append(this.initColVector, that.initColVector).append(this.rows, that.rows)
                .append(this.cols, that.cols).append(this.cyclicLength, that.cyclicLength).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(initColVector).
                append(rows).append(cols).append(cyclicLength).toHashCode();
    }
}