package edu.alibaba.mpc4j.dp.cdp.numeric.real.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.real.RealCdpConfig;

/**
 * 有界实数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public interface BoundRealCdpConfig extends RealCdpConfig {
    /**
     * 返回下边界。
     *
     * @return 下边界。
     */
    double getLowerBound();

    /**
     * 返回上边界。
     *
     * @return 上边界。
     */
    double getUpperBound();
}
