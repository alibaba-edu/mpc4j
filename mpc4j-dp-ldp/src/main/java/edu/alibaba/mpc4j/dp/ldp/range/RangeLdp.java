package edu.alibaba.mpc4j.dp.ldp.range;

import edu.alibaba.mpc4j.dp.ldp.Ldp;

/**
 * 范围LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public interface RangeLdp extends Ldp {
    /**
     * 随机化输入值。
     *
     * @param value 输入值。
     * @return 随机化结果。
     */
    double randomize(double value);

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
