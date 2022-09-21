package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;

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
     * 记录LDPC右矩阵两条复制对角线的相对位置
     */
    static final int[] BAND_POSITION = {5, 31};

    /**
     * 获取指定输出目标和LDPC类型对应的LPN参数。
     *
     * @param ceilLogN 目标输出OT数量的对数，支持[14, 24]。
     * @param codeType LDPC编码类型。
     * @return LPN参数。
     */
    public static LpnParams getLpnParams(int ceilLogN, CodeType codeType) {
        assert ceilLogN >= MIN_LOG_N && ceilLogN <= MAX_LOG_N
            : "log(n) must be in range [" + MIN_LOG_N + ", " + MAX_LOG_N + "]";
        switch (codeType) {
            case SILVER_5:
                return Silver5Utils.CRR21_SILVER5_LPN_PARAMS_MAP.get(ceilLogN);
            case SILVER_11:
                return Silver11Utils.CRR21_SILVER11_LPN_PARAMS_MAP.get(ceilLogN);
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
        }
    }

    /**
     * 获取右种子。
     *
     * @param codeType LDPC编码类型。
     * @return 右种子。
     */
    static int[][] getRightSeed(CodeType codeType) {
        switch (codeType) {
            case SILVER_5:
                return Silver5Utils.SILVER5_RIGHT_SEED;
            case SILVER_11:
                return Silver11Utils.SILVER11_RIGHT_SEED;
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
        }
    }

    /**
     * 获取改进后的右种子。
     *
     * @param codeType LDPC编码类型。
     * @return 改进后的右种子。
     */
    static int[][] getImprovedRightSeed(CodeType codeType) {
        switch (codeType) {
            case SILVER_5:
                return Silver5Utils.SILVER5_RIGHT_IMPROVED_SEED;
            case SILVER_11:
                return Silver11Utils.SILVER11_RIGHT_IMPROVED_SEED;
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
                return Silver5Utils.SILVER5_LEFT_SEED;
            case SILVER_11:
                return Silver11Utils.SILVER11_LEFT_SEED;
            default:
                throw new IllegalArgumentException("CodeType: " + codeType + "does not support!");
        }
    }

    /**
     * 获取预先计算已经存储的矩阵Ep。
     *
     * @param ceilLogN 目标输出OT数量的对数，支持支持[14, 24]。
     * @param codeType Ldpc类型。
     * @return 矩阵Ep。
     */
    public static byte[][] getMatrixEp(int ceilLogN, CodeType codeType) {
        assert ceilLogN >= MIN_LOG_N && ceilLogN <= MAX_LOG_N
            : "log(n) must be in range [" + MIN_LOG_N + ", " + MAX_LOG_N + "]";
        switch (codeType) {
            case SILVER_5:
                switch (ceilLogN) {
                    case 14:
                        return Silver5Utils.SILVER_5_M_EP_14;
                    case 15:
                        return Silver5Utils.SILVER_5_M_EP_15;
                    case 16:
                        return Silver5Utils.SILVER_5_M_EP_16;
                    case 17:
                        return Silver5Utils.SILVER_5_M_EP_17;
                    case 18:
                        return Silver5Utils.SILVER_5_M_EP_18;
                    case 19:
                        return Silver5Utils.SILVER_5_M_EP_19;
                    case 20:
                        return Silver5Utils.SILVER_5_M_EP_20;
                    case 21:
                        return Silver5Utils.SILVER_5_M_EP_21;
                    case 22:
                        return Silver5Utils.SILVER_5_M_EP_22;
                    case 23:
                        return Silver5Utils.SILVER_5_M_EP_23;
                    case 24:
                        return Silver5Utils.SILVER_5_M_EP_24;
                    default:
                        throw new IllegalArgumentException("log(n) must be in range [" + MIN_LOG_N + ", " + MAX_LOG_N + "]");
                }
            case SILVER_11:
                switch (ceilLogN) {
                    case 14:
                        return Silver11Utils.SILVER_11_M_EP_14;
                    case 15:
                        return Silver11Utils.SILVER_11_M_EP_15;
                    case 16:
                        return Silver11Utils.SILVER_11_M_EP_16;
                    case 17:
                        return Silver11Utils.SILVER_11_M_EP_17;
                    case 18:
                        return Silver11Utils.SILVER_11_M_EP_18;
                    case 19:
                        return Silver11Utils.SILVER_11_M_EP_19;
                    case 20:
                        return Silver11Utils.SILVER_11_M_EP_20;
                    case 21:
                        return Silver11Utils.SILVER_11_M_EP_21;
                    case 22:
                        return Silver11Utils.SILVER_11_M_EP_22;
                    case 23:
                        return Silver11Utils.SILVER_11_M_EP_23;
                    case 24:
                        return Silver11Utils.SILVER_11_M_EP_24;
                    default:
                        throw new IllegalArgumentException("log(n) must be in range [" + MIN_LOG_N + ", " + MAX_LOG_N + "]");
                }
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
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
                return Silver5Utils.GAP_VALUE;
            case SILVER_11:
                return Silver11Utils.GAP_VALUE;
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
                return Silver5Utils.WEIGHT;
            case SILVER_11:
                return Silver11Utils.WEIGHT;
            default:
                throw new IllegalArgumentException("Invalid CodeType: " + codeType.name());
        }
    }
}
