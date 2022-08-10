package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 实数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface RealLdpConfig extends LdpConfig {
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
