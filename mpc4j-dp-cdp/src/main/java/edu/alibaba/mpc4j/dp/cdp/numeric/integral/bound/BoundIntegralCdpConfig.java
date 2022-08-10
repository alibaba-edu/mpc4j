package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.IntegralCdpConfig;

/**
 * 有界整数CDP机制配置项。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/24
 */
public interface BoundIntegralCdpConfig extends IntegralCdpConfig {
    /**
     * 返回下边界。
     *
     * @return 下边界。
     */
    int getLowerBound();

    /**
     * 返回上边界。
     *
     * @return 上边界。
     */
    int getUpperBound();
}
