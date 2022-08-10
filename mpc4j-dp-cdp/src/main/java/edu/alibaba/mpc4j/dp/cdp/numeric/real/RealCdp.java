package edu.alibaba.mpc4j.dp.cdp.numeric.real;

import edu.alibaba.mpc4j.dp.cdp.Cdp;

/**
 * 实数CDP机制。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/20
 */
public interface RealCdp extends Cdp {
    /**
     * 返回敏感度Δf。
     *
     * @return 敏感度Δf。
     */
    default double getSensitivity() {
        return ((RealCdpConfig) getCdpConfig()).getSensitivity();
    }

    /**
     * 差分隐私处理。
     *
     * @param value 给定输入值。
     * @return 差分隐私处理结果。
     */
    double randomize(double value);
}
