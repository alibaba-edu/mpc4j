package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.IntegralCdp;

/**
 * 有界整数CDP机制。给定上界lowerBound和下界upperBound后，输入x ∈ [lowerBound, upperBound)，输出y ∈ [lowerBound, upperBound)。
 * <p>
 * 有界整数CDP机制可以等价为输入x ∈ [0, r)，输出y ∈ [0, r)，且输入和输出满足下述以距离定义评分函数的指数机制分布
 * （纵坐标表示输入，横坐标表示输出）：
 * </p>
 * <p>
 * |       |   0   |   1   |   2   |  ...  | r - 2 | r - 1 |
 * |   0   |   0   |  -1   |  -2   |  ...  | 2 - r | 1 - r |
 * |   1   |  -1   |   0   |  -1   |  ...  | 3 - r | 2 - r |
 * |  ...  |  ...  |  ...  |  ...  |  ...  |  ...  |  ...  |
 * | r - 2 | 2 - r | 3 - r | 4 - r |  ...  |   0   |  -1   |
 * | r - 1 | 1 - r | 2 - r | 3 - r |  ...  |  -1   |   0   |
 * </p>
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/24
 */
public interface BoundIntegralCdp extends IntegralCdp {
    /**
     * 返回下界。
     *
     * @return 下界。
     */
    default int getLowerBound() {
        return ((BoundIntegralCdpConfig)getCdpConfig()).getLowerBound();
    }

    /**
     * 返回上界。
     *
     * @return 上界。
     */
    default int getUpperBound() {
        return ((BoundIntegralCdpConfig)getCdpConfig()).getUpperBound();
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
