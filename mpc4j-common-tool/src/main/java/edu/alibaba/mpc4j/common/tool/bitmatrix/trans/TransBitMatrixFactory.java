package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 转置布尔矩阵工厂类。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class TransBitMatrixFactory {
    /**
     * 私有构造函数
     */
    private TransBitMatrixFactory() {
        // empty
    }

    /**
     * 比特矩阵类型
     */
    public enum TransBitMatrixType {
        /**
         * JDK transpose
         */
        JDK,
        /**
         * Eklundh转置布尔矩阵
         */
        EKLUNDH,
        /**
         * 本地转置布尔矩阵
         */
        NATIVE,
        /**
         * JDK行切分转置布尔矩阵
         */
        JDK_SPLIT_ROW,
        /**
         * 最优行切分转置布尔矩阵
         */
        NATIVE_SPLIT_ROW,
        /**
         * JDK列切分转置布尔矩阵
         */
        JDK_SPLIT_COL,
        /**
         * 最优列切分转置布尔矩阵
         */
        NATIVE_SPLIT_COL,
    }

    /**
     * 创建转置布尔矩阵实例。
     *
     * @param type    类型。
     * @param rows    行数。
     * @param columns 列数。
     * @return 实例。
     */
    public static TransBitMatrix createInstance(TransBitMatrixType type, int rows, int columns) {
        switch (type) {
            case JDK:
                return new JdkTransBitMatrix(rows, columns);
            case EKLUNDH:
                return new EklundhTransBitMatrix(rows, columns);
            case NATIVE:
                return new NativeTransBitMatrix(rows, columns);
            case JDK_SPLIT_ROW:
                return new JdkSplitRowTransBitMatrix(rows, columns);
            case NATIVE_SPLIT_ROW:
                return new NativeSplitRowTransBitMatrix(rows, columns);
            case JDK_SPLIT_COL:
                return new JdkSplitColTransBitMatrix(rows, columns);
            case NATIVE_SPLIT_COL:
                return new NativeSplitColTransBitMatrix(rows, columns);
            default:
                throw new IllegalArgumentException("Invalid " + TransBitMatrixType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建转置布尔矩阵实例。
     *
     * @param envType  环境类型。
     * @param rows     行数。
     * @param columns  列数。
     * @param parallel 是否并发处理。
     * @return 实例。
     */
    public static TransBitMatrix createInstance(EnvType envType, int rows, int columns, boolean parallel) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                if (parallel) {
                    // 并发处理，返回行切分或列切分
                    if (rows >= columns) {
                        return createInstance(TransBitMatrixType.NATIVE_SPLIT_ROW, rows, columns);
                    } else {
                        return createInstance(TransBitMatrixType.NATIVE_SPLIT_COL, rows, columns);
                    }
                } else {
                    // 串行处理
                    return createInstance(TransBitMatrixType.NATIVE, rows, columns);
                }
            case STANDARD_JDK:
            case INLAND_JDK:
                if (parallel) {
                    // 并发处理，返回行切分或列切分
                    if (rows >= columns) {
                        return createInstance(TransBitMatrixType.JDK_SPLIT_ROW, rows, columns);
                    } else {
                        return createInstance(TransBitMatrixType.JDK_SPLIT_COL, rows, columns);
                    }
                } else {
                    return createInstance(TransBitMatrixType.JDK, rows, columns);
                }
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
