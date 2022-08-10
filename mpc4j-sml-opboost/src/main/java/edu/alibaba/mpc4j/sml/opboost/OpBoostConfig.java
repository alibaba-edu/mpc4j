package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.common.tool.Config;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import smile.data.type.StructType;

import java.util.Map;

/**
 * OpBoost配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public interface OpBoostConfig extends Config {
    /**
     * 返回数据表格式。
     *
     * @return 数据表格式。
     */
    StructType getSchema();

    /**
     * 返回LDP配置项。
     *
     * @return LDP配置项映射。
     */
    Map<String, LdpConfig> getLdpConfigMap();
}
