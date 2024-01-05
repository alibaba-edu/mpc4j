package edu.alibaba.mpc4j.common.structure.lpn;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * Learning Parity with Noise (LPN)假设参数检查器。
 *
 * LPN参数包含3个变量(n_1, n_0, t)。其中，n_1表示输出长度n，n_0表示消息长度k，t表示噪声向量e的汉明重量。需要满足下述3个条件：
 * - Gaussian Elimination.
 * - Low-Weight Parity-Check.
 * - Inverse Syndrome Decoding.
 *
 * 相应条件来自于下述论文第5.1节：Minimizing Seed Size：
 * Boyle, Elette, Geoffroy Couteau, Niv Gilboa, and Yuval Ishai. Compressing vector OLE. CCS 2018, pp. 896-912. 2018.
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/01/26
 */
public class LpnParamsChecker {
    /**
     * 私有构造函数
     */
    private LpnParamsChecker() {
        // empty
    }

    /**
     * 验证LPN参数在λ = 128的条件下是否满足安全性要求。
     *
     * @param n1 输出长度。
     * @param n0 消息长度。
     * @param t 噪声汉明重量。
     * @return 如果满足安全性要求，返回{@code true}，否则返回{@code false}。
     */
    public static boolean validLpnParams(int n1, int n0, int t) {
        assert n0 > 0;
        // Low-Weight Parity-Check的参数要求
        assert n1 - n0 > 0;
        // Gaussian Elimination的参数要求
        assert t > 0 && t < n1;
        // 分别计算三种攻击的安全参数
        int gaussianCost = gaussianCost(n1, n0, t);
        int parityCheckCost = parityCheckCost(n1, n0, t);
        int isdCost = isdCost(n1, n0, t);
        return gaussianCost >= CommonConstants.BLOCK_BIT_LENGTH
            && parityCheckCost >= CommonConstants.BLOCK_BIT_LENGTH
            && isdCost >= CommonConstants.BLOCK_BIT_LENGTH;
    }

    /**
     * 计算Gaussian Elimination攻击下的安全参数。
     *
     * @param n1 输出长度。
     * @param n0 消息长度。
     * @param t 噪声汉明重量。
     * @return Gaussian Elimination攻击下的安全参数。
     */
    public static int gaussianCost(int n1, int n0, int t) {
        // log_2(n_0^2.8 * (1 / 1 - t / n_1)^n_0)
        return (int)Math.floor(DoubleUtils.log2(Math.pow(n0, 2.8) * Math.pow(1.0 / (1 - (double)t / n1), n0)));
    }

    /**
     * 计算Low-Weight Parity-Check攻击下的安全参数。
     *
     * @param n1 输出长度。
     * @param n0 消息长度。
     * @param t 噪声汉明重量。
     * @return Low-Weight Parity-Check攻击下的安全参数。
     */
    public static int parityCheckCost(int n1, int n0, int t) {
        // log_2((n_0 + 1) * (n_1 / (n_1 - n_0 - 1))^t)
        return (int)Math.floor(DoubleUtils.log2((n0 + 1) * Math.pow((double)n1 / (n1 - n0 - 1), t)));
    }

    /**
     * 计算Inverse Syndrome Decoding攻击下的安全参数。
     *
     * @param n1 输出长度。
     * @param n0 消息长度。
     * @param t 噪声汉明重量。
     * @return Inverse Syndrome Decoding攻击下的安全参数。
     */
    public static int isdCost(int n1, int n0, int t) {
        // log_2(C(n_1, t) / C((n_1 - n_0), t) * (n_1 - n_0)^2.8)
        return (int)Math.floor(DoubleUtils.log2(combinationDivide(n1, n0, t) * Math.pow(n1 - n0, 2.8)));
    }

    /**
     * 计算C(n_1, t) / C((n_1 - n_0), t)。如果先计算C(n_1, t)再计算C((n_1 - n_0), t)可能会溢出，因此把公式拆开一起计算。
     *
     * @param n1 输出长度。
     * @param n0 消息长度。
     * @param t 噪声汉明重量。
     * @return C(n_1, t) / C((n_1 - n_0), t)。
     */
    private static double combinationDivide(int n1, int n0, int t) {
        double result = 1.0;
        for (int i = 1; i <= t; i++) {
            result = result * ((n1 + 1 - i));
            result = result / (n1 - n0 + 1 - i);
        }
        // 一定能除尽，不需要保留小数
        return result;
    }
}
