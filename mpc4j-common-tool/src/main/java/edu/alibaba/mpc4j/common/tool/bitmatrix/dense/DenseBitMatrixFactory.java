package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import java.security.SecureRandom;

/**
 * dense bit matrix factory.
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class DenseBitMatrixFactory {
    /**
     * private constructor.
     */
    private DenseBitMatrixFactory() {
        // empty
    }

    /**
     * dense bit matrix type.
     */
    public enum DenseBitMatrixType {
        /**
         * long
         */
        LONG_MATRIX,
        /**
         * byte
         */
        BYTE_MATRIX,
    }

    /**
     * Creates a all-zero matrix.
     *
     * @param type    type.
     * @param rows    number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static DenseBitMatrix createAllZero(DenseBitMatrixType type, int rows, int columns) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteDenseBitMatrix.createAllZero(rows, columns);
            case LONG_MATRIX:
                return LongDenseBitMatrix.createAllZero(rows, columns);
            default:
                throw new IllegalArgumentException("Invalid " + DenseBitMatrixType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a all-one matrix.
     *
     * @param type    type.
     * @param rows    number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static DenseBitMatrix createAllOne(DenseBitMatrixType type, int rows, int columns) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteDenseBitMatrix.createAllOne(rows, columns);
            case LONG_MATRIX:
                return LongDenseBitMatrix.createAllOne(rows, columns);
            default:
                throw new IllegalArgumentException("Invalid " + DenseBitMatrixType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a all-one matrix.
     *
     * @param type    type.
     * @param rows    number of rows.
     * @param columns number of columns.
     * @param secureRandom random state.
     * @return a matrix.
     */
    public static DenseBitMatrix createRandom(DenseBitMatrixType type, int rows, int columns, SecureRandom secureRandom) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteDenseBitMatrix.createRandom(rows, columns, secureRandom);
            case LONG_MATRIX:
                return LongDenseBitMatrix.createRandom(rows, columns, secureRandom);
            default:
                throw new IllegalArgumentException("Invalid " + DenseBitMatrixType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a dense bit matrix from the dense data.
     *
     * @param type    type.
     * @param columns number of columns.
     * @param data    bit matrix data.
     * @return a dense bit matrix.
     */
    public static DenseBitMatrix createFromDense(DenseBitMatrixType type, int columns, byte[][] data) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteDenseBitMatrix.createFromDense(columns, data);
            case LONG_MATRIX:
                return LongDenseBitMatrix.createFromDense(columns, data);
            default:
                throw new IllegalArgumentException("Invalid " + DenseBitMatrixType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a dense bit matrix from the sparse positions.
     *
     * @param type      type.
     * @param columns   number of columns.
     * @param positions positions.
     * @return a dense bit matrix.
     */
    public static DenseBitMatrix createFromSparse(DenseBitMatrixType type, int columns, int[][] positions) {
        switch (type) {
            case BYTE_MATRIX:
                return ByteDenseBitMatrix.createFromSparse(columns, positions);
            case LONG_MATRIX:
                return LongDenseBitMatrix.createFromSparse(columns, positions);
            default:
                throw new IllegalArgumentException("Invalid " + DenseBitMatrixType.class.getSimpleName() + ": " + type);
        }
    }
}