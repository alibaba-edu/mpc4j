package edu.alibaba.mpc4j.dp.cdp.numeric.integral;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

/**
 * 整数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/21
 */
public interface IntegralCdpConfig extends CdpConfig {
    /**
     * 返回敏感度Δf。
     *
     * @return 敏感度Δf。
     */
    int getSensitivity();
}
