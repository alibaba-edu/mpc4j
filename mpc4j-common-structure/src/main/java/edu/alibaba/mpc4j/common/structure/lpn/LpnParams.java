package edu.alibaba.mpc4j.common.structure.lpn;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * LPN参数。
 *
 * @author Weiran Liu
 * @date 2022/01/27
 */
public class LpnParams {
    /**
     * n，输出长度
     */
    private int n;
    /**
     * k，消息长度
     */
    private int k;
    /**
     * t，噪声汉明重量
     */
    private int t;

    private LpnParams() {
        // empty
    }

    /**
     * 创建LPN参数，不检查参数的有效性。
     *
     * @param n 输出长度。
     * @param k 消息长度。
     * @param t 噪声汉明重量。
     * @return LPN参数。
     */
    public static LpnParams uncheckCreate(int n, int k, int t) {
        assert k > 0;
        // Low-Weight Parity-Check的参数要求
        assert n - k - 1 > 0;
        // Gaussian Elimination的参数要求
        assert t > 0 && t < n;

        LpnParams lpnParams = new LpnParams();
        lpnParams.n = n;
        lpnParams.k = k;
        lpnParams.t = t;

        return lpnParams;
    }

    /**
     * 创建LPN参数，检查参数的有效性。
     *
     * @param n 输出长度。
     * @param k 消息长度。
     * @param t 噪声汉明重量。
     * @return LPN参数。
     */
    public static LpnParams create(int n, int k, int t) {
        if (LpnParamsChecker.validLpnParams(n, k, t)) {
            return uncheckCreate(n, k, t);
        } else {
            throw new IllegalArgumentException(String.format(
                "Invalid LPN Params: GaussianCost = %s, Parity-Check Cost = %s, ISD Cost = %s",
                LpnParamsChecker.gaussianCost(n, k, t),
                LpnParamsChecker.parityCheckCost(n, k, t),
                LpnParamsChecker.isdCost(n, k, t)
            ));
        }
    }

    /**
     * 返回输出长度。
     *
     * @return 输出长度。
     */
    public int getN() {
        return n;
    }

    /**
     * 返回消息长度。
     *
     * @return 消息长度。
     */
    public int getK() {
        return k;
    }

    /**
     * 返回噪声汉明重量。
     *
     * @return 噪声汉明重量。
     */
    public int getT() {
        return t;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(n).append(k).append(t).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LpnParams) {
            LpnParams that = (LpnParams) obj;
            return new EqualsBuilder()
                .append(this.n, that.n)
                .append(this.k, that.k)
                .append(this.t, that.t)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("LPN params: (n = %s, k = %s, t = %s)", n, k, t);
    }
}
