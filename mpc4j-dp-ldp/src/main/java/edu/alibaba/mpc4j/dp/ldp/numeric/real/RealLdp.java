package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.dp.ldp.Ldp;

/**
 * 实数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface RealLdp extends Ldp {
    /**
     * 随机化输入值。
     *
     * @param value 输入值。
     * @return 随机化结果。
     */
    double randomize(double value);

    /**
     * 返回下界。
     *
     * @return 下界。
     */
    default double getLowerBound() {
        return ((RealLdpConfig) getLdpConfig()).getLowerBound();
    }

    /**
     * 返回上界。
     *
     * @return 上界。
     */
    default double getUpperBound() {
        return ((RealLdpConfig) getLdpConfig()).getUpperBound();
    }

    /**
     * 返回策略差分隐私参数。
     *
     * @param x1 第一个输入。
     * @param x2 第二个输入。
     * @return 策略差分隐私参数。
     */
    double getPolicyEpsilon(double x1, double x2);
}
