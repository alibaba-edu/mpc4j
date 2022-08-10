package edu.alibaba.mpc4j.dp.cdp.numeric.integral;

import edu.alibaba.mpc4j.dp.cdp.Cdp;

/**
 * 整数CDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/21
 */
public interface IntegralCdp extends Cdp {

    /**
     * 返回敏感度Δf。
     *
     * @return 敏感度Δf。
     */
    default int getSensitivity() {
        return ((IntegralCdpConfig)getCdpConfig()).getSensitivity();
    }

    /**
     * 差分隐私处理。
     *
     * @param value 给定输入值。
     * @return 差分隐私处理结果。
     */
    int randomize(int value);
}
