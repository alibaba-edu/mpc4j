package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.ldp.Ldp;

/**
 * 整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface IntegralLdp extends Ldp {
    /**
     * 随机化输入值。
     *
     * @param value 输入值。
     * @return 随机化结果。
     */
    int randomize(int value);

    /**
     * 返回下界。
     *
     * @return 下界。
     */
    default int getLowerBound() {
        return ((IntegralLdpConfig) getLdpConfig()).getLowerBound();
    }

    /**
     * 返回上界。
     *
     * @return 上界。
     */
    default int getUpperBound() {
        return ((IntegralLdpConfig) getLdpConfig()).getUpperBound();
    }

    /**
     * 返回策略差分隐私参数。
     *
     * @param x1 第一个输入。
     * @param x2 第二个输入。
     * @return 策略差分隐私参数。
     */
    double getPolicyEpsilon(int x1, int x2);
}
