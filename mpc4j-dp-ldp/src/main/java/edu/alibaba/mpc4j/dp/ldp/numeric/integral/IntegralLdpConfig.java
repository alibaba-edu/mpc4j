package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 整数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface IntegralLdpConfig extends LdpConfig {
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
