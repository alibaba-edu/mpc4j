package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 用byte[]表示的布尔方阵。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2021/12/20
 */
public class ByteSquareDenseBitMatrix extends AbstractByteDenseBitMatrix implements SquareDenseBitMatrix {
    /**
     * 构建布尔方阵。
     *
     * @param positions 布尔方阵中取值为1的位置。
     */
    public static ByteSquareDenseBitMatrix fromSparse(int[][] positions) {
        ByteSquareDenseBitMatrix squareDenseBitMatrix = new ByteSquareDenseBitMatrix();
        squareDenseBitMatrix.initFromSparse(positions.length, positions);
        return squareDenseBitMatrix;
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    public static ByteSquareDenseBitMatrix fromDense(byte[][] bitMatrix) {
       ByteSquareDenseBitMatrix squareDenseBitMatrix = new ByteSquareDenseBitMatrix();
       squareDenseBitMatrix.initFromDense(bitMatrix.length, bitMatrix);
       return squareDenseBitMatrix;
    }

    /**
     * 私有构造函数。
     */
    private ByteSquareDenseBitMatrix() {
        // empty
    }

    @Override
    public SquareDenseBitMatrix add(DenseBitMatrix that) {
        return fromDense(super.addToBytes(that));
    }

    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        return that.getColumns() == rows
            ? fromDense(super.multiplyToBytes(that))
            : ByteDenseBitMatrix.fromDense(that.getColumns(), super.multiplyToBytes(that));
    }

    @Override
    public SquareDenseBitMatrix transpose(EnvType envType, boolean parallel) {
        return fromDense(super.transposeToBytes(envType, parallel));
    }

    @Override
    public SquareDenseBitMatrix inverse() {
        // 构造布尔矩阵
        boolean[][] matrix = Arrays.stream(byteBitMatrix)
            .map(row -> BinaryUtils.byteArrayToBinary(row, rows))
            .toArray(boolean[][]::new);
        // 构造逆矩阵，先将逆矩阵初始化为单位阵
        boolean[][] inverseMatrix = new boolean[rows][rows];
        IntStream.range(0, rows).forEach(i -> inverseMatrix[i][i] = true);
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵
        for (int p = 0; p < rows; p++) {
            if (!matrix[p][p]) {
                // 找到一个不为0的行
                int other = p + 1;
                while (other < rows && !matrix[other][p]) {
                    other++;
                }
                if (other >= rows) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap
                    boolean[] matrixRowTemp = matrix[p];
                    matrix[p] = matrix[other];
                    matrix[other] = matrixRowTemp;
                    // 右侧矩阵行swap
                    boolean[] inverseMatrixRowTemp = inverseMatrix[p];
                    inverseMatrix[p] = inverseMatrix[other];
                    inverseMatrix[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元
            for (int i = p + 1; i < rows; i++) {
                if (matrix[i][p]) {
                    for (int j = 0; j < rows; j++) {
                        matrix[i][j] = matrix[i][j] ^ matrix[p][j];
                        inverseMatrix[i][j] = inverseMatrix[i][j] ^ inverseMatrix[p][j];
                    }
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵
        for (int p = rows - 1; p >= 0; p--) {
            for (int r = 0; r < p; r++) {
                if (matrix[r][p]) {
                    // 如果有1的，则进行相加
                    for (int j = 0; j < rows; j++) {
                        matrix[r][j] ^= matrix[p][j];
                        inverseMatrix[r][j] ^= inverseMatrix[p][j];
                    }
                }
            }
        }
        // 返回逆矩阵
        byte[][] invertByteBitMatrix = Arrays.stream(inverseMatrix)
            .map(BinaryUtils::binaryToRoundByteArray)
            .toArray(byte[][]::new);
        return ByteSquareDenseBitMatrix.fromDense(invertByteBitMatrix);
    }


    @Override
    public int getSize() {
        return rows;
    }

    @Override
    public int getByteSize() {
        return byteRows;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ByteSquareDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ByteSquareDenseBitMatrix that = (ByteSquareDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.byteBitMatrix, that.byteBitMatrix).isEquals();
    }
}
