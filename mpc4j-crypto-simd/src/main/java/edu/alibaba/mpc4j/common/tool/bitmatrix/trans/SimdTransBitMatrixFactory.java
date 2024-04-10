package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

/**
 * SIMD transpose bit matrix factory.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
public class SimdTransBitMatrixFactory {
    /**
     * private constructor.
     */
    private SimdTransBitMatrixFactory() {
        // empty
    }

    /**
     * SIMD transpose bit matrix type
     */
    public enum SimdTransBitMatrixType {
        /**
         * JDK
         */
        JDK,
        /**
         * JDK with row splitting.
         */
        JDK_SPLIT_ROW,
        /**
         * JDK with column splitting.
         */
        JDK_SPLIT_COL,
        /**
         * Vector API using 64-bit species.
         */
        VECTOR_64,
        /**
         * Vector API using 128-bit species.
         */
        VECTOR_128,
    }

    /**
     * 创建转置布尔矩阵实例。
     *
     * @param type    类型。
     * @param rows    行数。
     * @param columns 列数。
     * @return 实例。
     */
    public static SimdTransBitMatrix createInstance(SimdTransBitMatrixType type, int rows, int columns) {
        return switch (type) {
            case JDK -> new JdkSimdTransBitMatrix(rows, columns);
            case JDK_SPLIT_ROW -> new JdkSplitRowSimdTransBitMatrix(rows, columns);
            case JDK_SPLIT_COL -> new JdkSplitColSimdTransBitMatrix(rows, columns);
            case VECTOR_64 -> new Vector64SimdTransBitMatrix(rows, columns);
            case VECTOR_128 -> new Vector128SimdTransBitMatrix(rows, columns);
        };
    }
}
