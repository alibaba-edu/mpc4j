package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

/**
 * 稠密布尔方阵工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class SquareDenseBitMatrixFactory {
    /**
     * 稠密布尔方阵类型
     */
    public enum SquareDenseBitMatrixType {
        /**
         * 用长整数数组描述的布尔矩阵
         */
        LONG_MATRIX,
        /**
         * 用字节数组描述的布尔矩阵
         */
        BYTE_MATRIX,
    }

    /**
     * 私有构造函数
     */
    private SquareDenseBitMatrixFactory() {
        // empty
    }

    /**
     * 创建布尔方阵实例。
     *
     * @param type 布尔方阵类型。
     * @param bitMatrix 布尔方阵。
     * @return 布尔方阵实例。
     */
    public static SquareDenseBitMatrix fromDense(SquareDenseBitMatrixType type, byte[][] bitMatrix) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteSquareDenseBitMatrix.fromDense(bitMatrix);
            case LONG_MATRIX:
                return LongSquareDenseBitMatrix.fromDense(bitMatrix);
            default:
                throw new IllegalArgumentException("Invalid SquareBitMatrixType: " + type);
        }
    }

    /**
     * 创建布尔方阵实例。
     *
     * @param type 布尔方阵类型。
     * @param positions 布尔方阵中取值为1的位置。
     */
    public static SquareDenseBitMatrix fromSparse(SquareDenseBitMatrixType type, int[][] positions) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteSquareDenseBitMatrix.fromSparse(positions);
            case LONG_MATRIX:
                return LongSquareDenseBitMatrix.fromSparse(positions);
            default:
                throw new IllegalArgumentException("Invalid SquareBitMatrixType: " + type);
        }
    }
}