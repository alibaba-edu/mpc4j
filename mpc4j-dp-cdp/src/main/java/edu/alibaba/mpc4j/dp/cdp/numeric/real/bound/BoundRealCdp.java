package edu.alibaba.mpc4j.dp.cdp.numeric.real.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.real.RealCdp;

/**
 * 有界实数CDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public interface BoundRealCdp extends RealCdp {
    /**
     * 返回下界。
     *
     * @return 下界。
     */
    default double getLowerBound() {
        return ((BoundRealCdpConfig)getCdpConfig()).getLowerBound();
    }

    /**
     * 返回上界。
     *
     * @return 上界。
     */
    default double getUpperBound() {
        return ((BoundRealCdpConfig)getCdpConfig()).getUpperBound();
    }

    /**
     * 返回机制名称。
     *
     * @return 机制名称。
     */
    @Override
    default String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "], "
            + "(ε = " + getEpsilon() + ", δ = " + getDelta() + ")-"
            + getClass().getSimpleName();
    }
}
