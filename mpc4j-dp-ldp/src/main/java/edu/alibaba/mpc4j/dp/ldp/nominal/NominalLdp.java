package edu.alibaba.mpc4j.dp.ldp.nominal;

import edu.alibaba.mpc4j.dp.ldp.Ldp;

/**
 * 枚举LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public interface NominalLdp extends Ldp {
    /**
     * 返回LDP机制所实现的ε。
     *
     * @return ε值。
     */
    double getEpsilon();

    /**
     * 返回机制名称。
     *
     * @return 机制名称。
     */
    @Override
    default String getMechanismName() {
        return "(ε = " + getEpsilon() + ")-" + getClass().getSimpleName();
    }
}
