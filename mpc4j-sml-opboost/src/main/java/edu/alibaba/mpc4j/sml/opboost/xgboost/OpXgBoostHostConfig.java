package edu.alibaba.mpc4j.sml.opboost.xgboost;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.AbstractOpBoostConfigBuilder;
import edu.alibaba.mpc4j.sml.opboost.OpBoostHostConfig;
import smile.data.type.StructType;

import java.util.Map;

/**
 * OpXgBoost主机配置参数。
 *
 * @author Weiran Liu
 * @date 2021/10/09
 */
public class OpXgBoostHostConfig implements OpBoostHostConfig {
    /**
     * 数据格式
     */
    private final StructType schema;
    /**
     * 配置参数
     */
    private final XgBoostParams xgBoostParams;
    /**
     * LDP机制配置项
     */
    private final Map<String, LdpConfig> ldpConfigMap;

    private OpXgBoostHostConfig(Builder builder) {
        schema = builder.getSchema();
        ldpConfigMap = builder.getLdpConfigMap();
        // 设置训练参数
        xgBoostParams = builder.xgBoostParams;
    }

    @Override
    public StructType getSchema() {
        return schema;
    }

    public XgBoostParams getXgBoostParams() {
        return xgBoostParams;
    }

    @Override
    public Map<String, LdpConfig> getLdpConfigMap() {
        return ldpConfigMap;
    }

    public static class Builder extends AbstractOpBoostConfigBuilder<OpXgBoostHostConfig> {
        /**
         * Gradient Boost Classification parameters
         */
        private final XgBoostParams xgBoostParams;

        public Builder(StructType schema, XgBoostParams xgBoostParams) {
            super(schema);
            this.xgBoostParams = xgBoostParams;
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
        public OpXgBoostHostConfig build() {
            return new OpXgBoostHostConfig(this);
        }
    }
}
