package edu.alibaba.mpc4j.dp.cdp.nominal;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

import java.util.Map;
import java.util.Set;

/**
 * 枚举CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public interface NominalCdpConfig extends CdpConfig {
    /**
     * 返回所有可能的枚举值集合。
     *
     * @return 所有可能的枚举值集合。
     */
    Set<String> getNounSet();

    /**
     * 返回评分函数。
     *
     * @return 评分函数。
     */
    Map<NounPair, Double> getUtilityMap();

    /**
     * 返回Δq。
     *
     * @return Δq。
     */
    double getDeltaQ();
}
