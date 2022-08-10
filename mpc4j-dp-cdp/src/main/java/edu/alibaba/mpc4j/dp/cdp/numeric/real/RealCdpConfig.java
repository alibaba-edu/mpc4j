package edu.alibaba.mpc4j.dp.cdp.numeric.real;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

/**
 * 实数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public interface RealCdpConfig extends CdpConfig {
    /**
     * 返回敏感度Δf。
     *
     * @return 敏感度Δf。
     */
    double getSensitivity();
}
