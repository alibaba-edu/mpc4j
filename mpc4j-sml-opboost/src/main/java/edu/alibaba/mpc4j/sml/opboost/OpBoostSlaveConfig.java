package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import smile.data.type.StructType;

import java.util.Map;

/**
 * OpBoost从机配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/28
 */
public class OpBoostSlaveConfig implements OpBoostConfig {
    /**
     * 数据格式
     */
    private final StructType schema;
    /**
     * LDP机制配置项
     */
    private final Map<String, LdpConfig> ldpConfigMap;

    private OpBoostSlaveConfig(Builder builder) {
        schema = builder.getSchema();
        ldpConfigMap = builder.getLdpConfigMap();
    }

    @Override
    public StructType getSchema() {
        return schema;
    }

    @Override
    public Map<String, LdpConfig> getLdpConfigMap() {
        return ldpConfigMap;
    }

    public static class Builder extends AbstractOpBoostConfigBuilder<OpBoostSlaveConfig> {

        public Builder(StructType schema) {
            super(schema);
        }

        @Override
        public Builder addLdpConfig(Map<String, LdpConfig> ldpConfigMap) {
            return (Builder) super.addLdpConfig(ldpConfigMap);
        }

        @Override
        public Builder addLdpConfig(String name, LdpConfig ldpConfig) {
            return (Builder) super.addLdpConfig(name, ldpConfig);
        }

        @Override
        public OpBoostSlaveConfig build() {
            return new OpBoostSlaveConfig(this);
        }
    }
}
