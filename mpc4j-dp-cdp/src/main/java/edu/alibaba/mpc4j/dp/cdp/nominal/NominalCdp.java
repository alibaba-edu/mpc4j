package edu.alibaba.mpc4j.dp.cdp.nominal;

import edu.alibaba.mpc4j.dp.cdp.Cdp;

/**
 * 枚举CDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public interface NominalCdp extends Cdp {

    /**
     * 返回Δq。
     *
     * @return Δq。
     */
    default double getDeltaQ() {
        return ((NominalCdpConfig)getCdpConfig()).getDeltaQ();
    }

    /**
     * 差分隐私处理。
     *
     * @param noun 给定输入值。
     * @return 差分隐私处理结果。
     */
    String randomize(String noun);

    /**
     * 返回机制名称。
     *
     * @return 机制名称。
     */
    @Override
    default String getMechanismName() {
        return "(ε = " + getEpsilon()
            + ", δ = " + getDelta()
            + ", Δq = " + getDeltaQ()
            + ")-" + getClass().getSimpleName();
    }
}
