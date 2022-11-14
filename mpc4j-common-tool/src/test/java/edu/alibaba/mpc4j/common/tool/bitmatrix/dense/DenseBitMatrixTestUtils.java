package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.SquareDenseBitMatrixFactory.SquareDenseBitMatrixType;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 稠密布尔矩阵测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/8/3
 */
public class DenseBitMatrixTestUtils {
    /**
     * 私有构造函数
     */
    private DenseBitMatrixTestUtils() {
        // empty
    }

    /**
     * 创建全0稠密方阵。
     *
     * @param type 类型。
     * @param size 大小。
     * @return 稠密方阵。
     */
    static SquareDenseBitMatrix createAllZero(SquareDenseBitMatrixType type, int size) {
        int byteSize = CommonUtils.getByteLength(size);
        return SquareDenseBitMatrixFactory.fromDense(type, new byte[size][byteSize]);
    }

    /**
     * 创建全0稠密矩阵。
     *
     * @param rows 行数。
     * @param columns 列数。
     * @return 稠密矩阵。
     */
    static DenseBitMatrix createAllZero(int rows, int columns) {
        int byteColumns = CommonUtils.getByteLength(columns);
        return ByteDenseBitMatrix.fromDense(columns, new byte[rows][byteColumns]);
    }

    /**
     * 创建全0向量。
     *
     * @param bitLength 比特长度。
     * @return 全0向量。
     */
    static byte[] createAllZero(int bitLength) {
        int byteLength = CommonUtils.getByteLength(bitLength);
        return new byte[byteLength];
    }

    /**
     * 创建全1稠密方阵。
     *
     * @param type 类型。
     * @param size 大小。
     * @return 稠密方阵。
     */
    static SquareDenseBitMatrix createAllOne(SquareDenseBitMatrixType type, int size) {
        int byteSize = CommonUtils.getByteLength(size);
        byte[][] byteBitMatrix = IntStream.range(0, size)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteSize];
                Arrays.fill(row, (byte) 0xFF);
                BytesUtils.reduceByteArray(row, size);
                return row;
            })
            .toArray(byte[][]::new);
        return SquareDenseBitMatrixFactory.fromDense(type, byteBitMatrix);
    }

    /**
     * 创建全1稠密矩阵。
     *
     * @param rows 行数。
     * @param columns 列数。
     * @return 稠密矩阵。
     */
    static DenseBitMatrix createAllOne(int rows, int columns) {
        int byteColumns = CommonUtils.getByteLength(columns);
        byte[][] byteBitMatrix = IntStream.range(0, rows)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteColumns];
                Arrays.fill(row, (byte) 0xFF);
                BytesUtils.reduceByteArray(row, columns);
                return row;
            })
            .toArray(byte[][]::new);
        return ByteDenseBitMatrix.fromDense(columns, byteBitMatrix);
    }

    /**
     * 创建全1向量。
     *
     * @param bitLength 比特长度。
     * @return 全1向量。
     */
    static byte[] createAllOne(int bitLength) {
        int byteLength = CommonUtils.getByteLength(bitLength);
        byte[] vector = new byte[byteLength];
        Arrays.fill(vector, (byte) 0xFF);
        BytesUtils.reduceByteArray(vector, bitLength);
        return vector;
    }

    /**
     * 创建随机稠密方阵。
     *
     * @param type 类型。
     * @param size 大小。
     * @param secureRandom 随机状态。
     * @return 方阵。
     */
    public static SquareDenseBitMatrix createRandom(SquareDenseBitMatrixType type, int size, SecureRandom secureRandom) {
        int byteSize = CommonUtils.getByteLength(size);
        byte[][] byteBitMatrix = IntStream.range(0, size)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteSize];
                secureRandom.nextBytes(row);
                BytesUtils.reduceByteArray(row, size);
                return row;
            })
            .toArray(byte[][]::new);
        return SquareDenseBitMatrixFactory.fromDense(type, byteBitMatrix);
    }

    /**
     * 创建随机稠密矩阵。
     *
     * @param rows 行数。
     * @param columns 列数。
     * @param secureRandom 随机状态。
     * @return 稠密矩阵。
     */
    public static DenseBitMatrix createRandom(int rows, int columns, SecureRandom secureRandom) {
        int byteColumns = CommonUtils.getByteLength(columns);
        byte[][] byteBitMatrix = IntStream.range(0, rows)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteColumns];
                secureRandom.nextBytes(row);
                BytesUtils.reduceByteArray(row, columns);
                return row;
            })
            .toArray(byte[][]::new);
        return ByteDenseBitMatrix.fromDense(columns, byteBitMatrix);
    }

    /**
     * 创建随机向量。
     *
     * @param bitLength 比特长度。
     * @param secureRandom 随机状态。
     * @return 随机向量。
     */
    static byte[] createRandom(int bitLength, SecureRandom secureRandom) {
        int byteColumns = CommonUtils.getByteLength(bitLength);
        byte[] row = new byte[byteColumns];
        secureRandom.nextBytes(row);
        BytesUtils.reduceByteArray(row, bitLength);
        return row;
    }
}
