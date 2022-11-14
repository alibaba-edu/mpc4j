package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

/**
 * LdpcCreator类，提供公开方法获取指定LDPC类型的相关参数。
 *
 * @author Hanwen Feng
 * @date 2022/03/12
 */
public class LdpcCreatorUtils {
    /**
     * LDPC编码支持的最小log(n)
     */
    public static final int MIN_LOG_N = 14;
    /**
     * LDPC编码支持的最大log(n)
     */
    public static final int MAX_LOG_N = 24;

    /**
     * LDPC编码类型
     */
    public enum CodeType {
        /**
         * Silver5编码
         */
        SILVER_5,
        /**
         * Silver11编码
         */
        SILVER_11,
    }

    /**
     * 获取右种子。
     *
     * @param codeType LDPC编码类型。
     * @return 改进后的右种子。
     */
    static int[][] getRightSeed(CodeType codeType) {
        switch (codeType) {
            case SILVER_5:
                return SILVER5_RIGHT_SEED;
            case SILVER_11:
                return SILVER11_RIGHT_SEED;
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
        }
    }

    /**
     * 获取左种子。
     *
     * @param codeType LDPC编码类型。
     * @return 左种子。
     */
    static double[] getLeftSeed(CodeType codeType) {
        switch (codeType) {
            case SILVER_5:
                return SILVER5_LEFT_SEED;
            case SILVER_11:
                return SILVER11_LEFT_SEED;
            default:
                throw new IllegalArgumentException("CodeType: " + codeType + "does not support!");
        }
    }

    /**
     * 获取LDPC的参数：gap。
     *
     * @param codeType LDPC编码类型。
     * @return 参数：gap。
     */
    static int getGap(CodeType codeType) {
        switch (codeType) {
            case SILVER_5:
                return SILVER5_GAP_VALUE;
            case SILVER_11:
                return SILVER11_GAP_VALUE;
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
        }
    }

    /**
     * 获取LDPC的参数：weight。
     *
     * @param codeType LDPC编码类型。
     * @return 参数：weight。
     */
    static int getWeight(CodeType codeType) {
        switch (codeType) {
            case SILVER_5:
                return SILVER5_WEIGHT;
            case SILVER_11:
                return SILVER11_WEIGHT;
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
        }
    }

    /**
     * Silver5 gap值
     */
    private static final int SILVER5_GAP_VALUE = 16;
    /**
     * Silver11 gap值
     */
    private static final int SILVER11_GAP_VALUE = 32;
    /**
     * Silver5 左矩阵每列的汉明重量
     */
    private static final int SILVER5_WEIGHT = 5;
    /**
     * Silver 11 左矩阵每列的汉明重量
     */
    private static final int SILVER11_WEIGHT = 11;
    /**
     * Silver5左矩阵种子
     * 左矩阵是循环矩阵，种子为第一列非零点坐标的百分位。
     */
    private static final double[] SILVER5_LEFT_SEED = {0,
        0.372071, 0.576568, 0.608917, 0.854475};
    /**
     * Silver11左矩阵种子
     * 左矩阵是循环矩阵，种子为第一列非零点坐标的百分位
     */
    private static final double[] SILVER11_LEFT_SEED = {0,
        0.00278835, 0.0883852, 0.238023, 0.240532,
        0.274624, 0.390639, 0.531551, 0.637619,
        0.945265, 0.965874};
    /**
     * Silver5右矩阵种子
     * 改进的右种子，是右矩阵每列的非零点，按周期移位重复。
     */
    private static final int[][] SILVER5_RIGHT_SEED = {
        {0, 2, 11, 14, 16, 21, 47},
        {0, 3, 8, 9, 16, 21, 47},
        {0, 1, 2, 4, 8, 21, 47},
        {0, 1, 10, 15, 16, 21, 47},
        {0, 7, 8, 12, 14, 21, 47},
        {0, 1, 2, 9, 10, 21, 47},
        {0, 2, 3, 8, 16, 21, 47},
        {0, 5, 6, 13, 14, 21, 47},
        {0, 3, 4, 11, 15, 21, 47},
        {0, 4, 6, 8, 12, 21, 47},
        {0, 2, 3, 7, 15, 21, 47},
        {0, 3, 5, 6, 8, 21, 47},
        {0, 6, 9, 12, 13, 21, 47},
        {0, 8, 10, 11, 13, 21, 47},
        {0, 1, 10, 12, 13, 21, 47},
        {0, 1, 7, 8, 16, 21, 47},
    };
    /**
     * Silver11右矩阵种子
     * 改进的右种子，是右矩阵每列的非零点，按周期移位重复。
     */
    private static final int[][] SILVER11_RIGHT_SEED = {
        {0, 1, 2, 4, 15, 16, 17, 19, 24, 25, 26, 37, 63},
        {0, 1, 9, 10, 12, 18, 23, 24, 25, 30, 32, 37, 63},
        {0, 5, 6, 13, 14, 15, 16, 18, 19, 24, 31, 37, 63},
        {0, 2, 11, 14, 15, 19, 20, 22, 24, 28, 31, 37, 63},
        {0, 2, 3, 5, 6, 8, 19, 21, 22, 23, 27, 37, 63},
        {0, 1, 6, 8, 10, 12, 13, 16, 17, 21, 26, 37, 63},
        {0, 2, 4, 7, 8, 10, 24, 25, 26, 28, 29, 37, 63},
        {0, 4, 8, 10, 12, 13, 15, 23, 25, 26, 29, 37, 63},
        {0, 2, 6, 7, 8, 12, 17, 24, 27, 29, 30, 37, 63},
        {0, 2, 4, 5, 11, 12, 17, 19, 21, 22, 25, 37, 63},
        {0, 2, 5, 8, 9, 11, 12, 18, 20, 28, 32, 37, 63},
        {0, 4, 6, 8, 12, 17, 18, 19, 20, 22, 27, 37, 63},
        {0, 9, 16, 17, 20, 22, 23, 24, 27, 29, 30, 37, 63},
        {0, 3, 5, 7, 16, 17, 18, 20, 23, 30, 32, 37, 63},
        {0, 1, 2, 5, 7, 8, 9, 11, 19, 20, 28, 37, 63},
        {0, 3, 9, 11, 13, 15, 24, 27, 28, 31, 32, 37, 63},
        {0, 5, 10, 13, 14, 16, 19, 21, 26, 29, 32, 37, 63},
        {0, 1, 4, 7, 10, 12, 15, 20, 21, 24, 26, 37, 63},
        {0, 6, 10, 13, 15, 16, 17, 18, 19, 20, 21, 37, 63},
        {0, 4, 6, 7, 8, 18, 20, 24, 25, 27, 30, 37, 63},
        {0, 1, 4, 12, 13, 19, 25, 29, 30, 31, 32, 37, 63},
        {0, 2, 9, 15, 16, 19, 22, 23, 25, 28, 29, 37, 63},
        {0, 5, 7, 10, 15, 19, 21, 24, 26, 29, 30, 37, 63},
        {0, 4, 6, 11, 13, 15, 21, 25, 29, 31, 32, 37, 63},
        {0, 8, 10, 11, 15, 16, 17, 25, 27, 30, 32, 37, 63},
        {0, 3, 4, 5, 7, 14, 15, 16, 20, 28, 32, 37, 63},
        {0, 2, 3, 9, 11, 14, 15, 18, 19, 20, 25, 37, 63},
        {0, 10, 12, 13, 17, 19, 23, 27, 28, 29, 32, 37, 63},
        {0, 7, 8, 10, 12, 13, 16, 26, 28, 29, 31, 37, 63},
        {0, 6, 7, 11, 12, 15, 23, 27, 28, 30, 31, 37, 63},
        {0, 5, 6, 8, 10, 11, 14, 22, 25, 29, 30, 37, 63},
        {0, 2, 11, 14, 16, 17, 23, 24, 27, 30, 32, 37, 63},
    };
}
