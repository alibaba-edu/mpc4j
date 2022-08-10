package edu.alibaba.mpc4j.common.tool.lpn.matrix;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 普通的0-1矩阵类型，主要用于测试SparseMtx的实现是否正确, 以及支持Ldpc Encoder的计算
 *
 * @author Hanwen Feng
 * @date 2022.1.10
 */

public class DenseMatrix {
    /**
     * 矩阵信息存储为byte[][]
     * pureMatrix[i] 存储矩阵的第i 行
     * 每一个byte 仅存储 0 或者1
     */
    private final byte[][] pureMatrix;
    /**
     * 行数
     */
    private final int rows;
    /**
     * 列数
     */
    private final int cols;

    /**
     * 将给定的byte[][] 封装为DenseMatrix
     *
     * @param rows   行数
     * @param cols   列数
     * @param matrix pureMatrix
     */
    public DenseMatrix(int rows, int cols, byte[][] matrix) {
        assert (rows == matrix.length);
        assert (cols == matrix[0].length);

        this.rows = rows;
        this.cols = cols;
        pureMatrix = matrix;
    }

    /**
     * 计算两个DenseMatrix相加
     *
     * @param nDMtx 待加的DenseMatrix
     * @return 返回加和
     */
    public DenseMatrix add(DenseMatrix nDMtx) {
        assert (rows == nDMtx.rows);
        assert (cols == nDMtx.cols);

        int rRowSize = rows;
        int rColSize = cols;

        byte[][] rMTx = new byte[rRowSize][rColSize];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rMTx[i][j] = (byte) (pureMatrix[i][j] ^ nDMtx.pureMatrix[i][j]);
            }
        }
        return new DenseMatrix(rRowSize, rColSize, rMTx);
    }

    /**
     * 计算两个DenseMatrix相乘
     *
     * @param dMtx 待乘矩阵
     * @return 乘积
     */
    public DenseMatrix multi(DenseMatrix dMtx) {
        assert (cols == dMtx.rows);

        int rRowsize = rows;
        int rColSize = cols;

        byte[][] rMtx = new byte[rRowsize][rColSize];

        for (int i = 0; i < rRowsize; i++) {
            for (int j = 0; j < rColSize; j++) {
                int temp = 0;
                for (int k = 0; k < cols; k++) {
                    temp ^= (pureMatrix[i][k] & dMtx.pureMatrix[k][j]);
                }
                rMtx[i][j] = (byte) temp;
            }
        }
        return new DenseMatrix(rRowsize, rColSize, rMtx);
    }

    /**
     * 对于可逆方阵，计算逆矩阵
     *
     * @return 返回逆矩阵
     */
    public DenseMatrix getInverse() {
        assert rows == cols;
        byte[][] matrix = new byte[rows][cols];
        IntStream.range(0, rows).forEach(i -> matrix[i] = Arrays.copyOf(pureMatrix[i], cols));
        // 构造逆矩阵，先将逆矩阵初始化为单位阵。
        byte[][] inverseMatrix = new byte[cols][rows];
        IntStream.range(0, rows).forEach(i -> inverseMatrix[i][i] = 1);
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵。
        for (int p = 0; p < rows; p++) {
            if (matrix[p][p] == 0) {
                // 找到一个不为0的行。
                int other = p + 1;
                while (other < rows && matrix[other][p] == 0) {
                    other++;
                }
                if (other >= rows) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap。
                    byte[] matrixRowTemp = matrix[p];
                    matrix[p] = matrix[other];
                    matrix[other] = matrixRowTemp;
                    // 右侧矩阵行swap。
                    byte[] inverseMatrixRowTemp = inverseMatrix[p];
                    inverseMatrix[p] = inverseMatrix[other];
                    inverseMatrix[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元。
            for (int i = p + 1; i < rows; i++) {
                if (matrix[i][p] == 1) {
                    for (int j = 0; j < rows; j++) {
                        matrix[i][j] = (byte) (matrix[i][j] ^ matrix[p][j]);
                        inverseMatrix[i][j] = (byte) (inverseMatrix[i][j] ^ inverseMatrix[p][j]);
                    }
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵。
        for (int p = rows - 1; p >= 0; p--) {
            for (int r = 0; r < p; r++) {
                if (matrix[r][p] == 1) {
                    // 如果有1的，则进行相加。
                    for (int j = 0; j < rows; j++) {
                        matrix[r][j] ^= matrix[p][j];
                        inverseMatrix[r][j] ^= inverseMatrix[p][j];
                    }
                }
            }
        }
        // 返回逆矩阵。
        return new DenseMatrix(cols, rows, inverseMatrix);
    }

    /**
     * 计算byte[]右乘矩阵 M*e
     *
     * @param vec 布尔向量
     * @return 返回结果
     */
    public byte[] rmul(final byte[] vec) {
        byte[] rVec = new byte[rows];
        rmulAdd(vec, rVec);
        return rVec;
    }

    /**
     * 计算布尔向量右乘矩阵 M*e
     *
     * @param vec 布尔向量
     * @return 返回结果
     */
    public boolean[] rmul(final boolean[] vec) {
        boolean[] rVec = new boolean[rows];
        rmulAdd(vec, rVec);
        return rVec;
    }

    /**
     * 计算byte[][]右乘矩阵 M*e
     *
     * @param vec 布尔向量
     * @return 返回结果
     */
    public byte[][] rmul(final byte[][] vec) {
        byte[][] rVec = new byte[rows][vec[0].length];
        rmulAdd(vec, rVec);
        return rVec;
    }

    /**
     * 计算byte[]右乘矩阵再加向量 M *x+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void rmulAdd(final byte[] vecX, byte[] vecY) {
        assert cols == vecX.length;
        assert rows == vecY.length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (pureMatrix[i][j] == 1) {
                    vecY[i] = (byte) (vecY[i] ^ vecX[j]);
                }
            }
        }
    }

    /**
     * 计算byte[][]右乘矩阵再加向量 M *x+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void rmulAdd(final byte[][] vecX, byte[][] vecY) {
        assert cols == vecX.length;
        assert rows == vecY.length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (pureMatrix[i][j] == 1) {
                    BytesUtils.xori(vecY[i], vecX[j]);
                }
            }
        }
    }

    /**
     * 计算byte[]右乘矩阵再加向量 M *x+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void rmulAdd(final boolean[] vecX, boolean[] vecY) {
        assert cols == vecX.length;
        assert rows == vecY.length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (pureMatrix[i][j] == 1) {
                    vecY[i] = (vecY[i] ^ vecX[j]);
                }
            }
        }
    }

    /**
     * 计算byte[]左乘矩阵 e*M
     *
     * @param vec 向量
     * @return 返回结果
     */
    public byte[] lmul(final byte[] vec) {
        byte[] vecY = new byte[cols];
        lmulAdd(vec, vecY);
        return vecY;
    }

    /**
     * 计算布尔向量左乘矩阵 e*M
     *
     * @param vec 布尔向量
     * @return 返回结果
     */
    public boolean[] lmul(final boolean[] vec) {
        boolean[] vecY = new boolean[cols];
        lmulAdd(vec, vecY);
        return vecY;
    }

    /**
     * 计算byte[][] 左乘矩阵 e*M
     *
     * @param vecX 向量
     * @return 返回结果
     */
    public byte[][] lmul(final byte[][] vecX) {
        assert getRows() == vecX.length;
        int byteLength = vecX[0].length;
        byte[][] vecY = new byte[cols][byteLength];
        lmulAdd(vecX, vecY);
        return vecY;
    }

    /**
     * 计算布尔左乘矩阵再加向量 x*M+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void lmulAdd(boolean[] vecX, boolean[] vecY) {
        assert rows == vecX.length;
        assert cols == vecY.length;

        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows; i++) {
                if (pureMatrix[i][j] == 1) {
                    vecY[j] ^= vecX[i];
                }
            }
        }
    }

    /**
     * 计算byte[]左乘矩阵再加向量 x*M+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void lmulAdd(byte[] vecX, byte[] vecY) {
        assert rows == vecX.length;
        assert cols == vecY.length;

        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows; i++) {
                if (pureMatrix[i][j] == 1) {
                    vecY[j] ^= vecX[i];
                }
            }
        }
    }

    /**
     * 计算byte[][]左乘矩阵再加向量 x*M+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void lmulAdd(byte[][] vecX, byte[][] vecY) {
        assert rows == vecX.length;
        assert cols == vecY.length;

        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows; i++) {
                if (pureMatrix[i][j] == 1) {
                    BytesUtils.xori(vecY[j], vecX[i]);
                }
            }
        }
    }

    /**
     * 读取列数
     *
     * @return 列数
     */
    public int getCols() {
        return cols;
    }

    /**
     * 读取行数
     *
     * @return 行数
     */
    public int getRows() {
        return rows;
    }

    /**
     * 读取指定位置的值
     *
     * @param rowIndex 行坐标
     * @param colIndex 列坐标
     * @return 值
     */
    public boolean getValue(int rowIndex, int colIndex) {
        assert rowIndex < rows && colIndex < cols;

        return pureMatrix[rowIndex][colIndex] == 1;
    }

    /**
     * 读取byte[][]存储的矩阵
     *
     * @return pureMatrix
     */
    public byte[][] getPureMatrix() {
        return pureMatrix;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DenseMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        DenseMatrix that = (DenseMatrix) obj;
        return new EqualsBuilder().append(this.pureMatrix, that.pureMatrix).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(pureMatrix).toHashCode();
    }


}
