package edu.alibaba.mpc4j.sml.opboost.grad;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.AbstractOpBoostConfigBuilder;
import edu.alibaba.mpc4j.sml.opboost.OpBoostHostConfig;
import smile.data.type.StructType;

import java.util.Map;
import java.util.Properties;

/**
 * 分类OpGradBoost主机配置参数。
 *
 * @author Weiran Liu
 * @date 2021/09/26
 */
public class ClsOpGradBoostHostConfig implements OpBoostHostConfig {
    /**
     * 数据格式
     */
    private final StructType schema;
    /**
     * Smile配置参数
     */
    private final Properties smileProperties;
    /**
     * LDP机制配置项
     */
    private final Map<String, LdpConfig> ldpConfigMap;

    private ClsOpGradBoostHostConfig(Builder builder) {
        schema = builder.getSchema();
        ldpConfigMap = builder.getLdpConfigMap();
        // 设置训练参数
        smileProperties = new Properties();
        smileProperties.setProperty("smile.gbt.sample.rate", String.valueOf(builder.sampleRate));
        smileProperties.setProperty("smile.gbt.max.depth", String.valueOf(builder.maxDepth));
        smileProperties.setProperty("smile.gbt.max.nodes", String.valueOf(builder.maxNodes));
        smileProperties.setProperty("smile.gbt.node.size", String.valueOf(builder.nodeSize));
        smileProperties.setProperty("smile.gbt.shrinkage", String.valueOf(builder.shrinkage));
        smileProperties.setProperty("smile.gbt.trees", String.valueOf(builder.treeNum));
    }

    @Override
    public StructType getSchema() {
        return schema;
    }

    @Override
    public Map<String, LdpConfig> getLdpConfigMap() {
        return ldpConfigMap;
    }

    public Properties getSmileProperties() {
        return smileProperties;
    }

    public static class Builder extends AbstractOpBoostConfigBuilder<ClsOpGradBoostHostConfig> {
        /**
         * 树的总数量
         */
        private int treeNum;
        /**
         * 树的最大深度
         */
        private int maxDepth;
        /**
         * 树的最大节点数
         */
        private int maxNodes;
        /**
         * 树节点包含的最小样本数量
         */
        private int nodeSize;
        /**
         * 收缩率
         */
        private double shrinkage;
        /**
         * 采样率
         */
        private double sampleRate;

        public Builder(StructType schema) {
            super(schema);
            // 训练参数遵循XGBoost的默认值
            treeNum = 100;
            maxDepth = 3;
            maxNodes = 2;
            nodeSize = 1;
            shrinkage = 0.1;
            sampleRate = 1.0;
        }

        public Builder setTreeNum(int treeNum) {
            assert treeNum > 0 : "treeNum must be greater than 0";
            this.treeNum = treeNum;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            assert maxDepth >= 1 : "maxDepth must be greater or equal than 1";
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setMaxNodes(int maxNodes) {
            assert maxNodes >= 2 : "maxNode must be greater or equal than 2";
            this.maxNodes = maxNodes;
            return this;
        }

        public Builder setNodeSize(int nodeSize) {
            assert nodeSize >= 1 : "nodeSize must be greater or equal than 1";
            this.nodeSize = nodeSize;
            return this;
        }

        public Builder setShrinkage(double shrinkage) {
            assert shrinkage > 0 && shrinkage <= 1 : "shrinkage must be in range (0, 1]";
            this.shrinkage = shrinkage;
            return this;
        }

        public Builder setSampleRate(double sampleRate) {
            assert sampleRate > 0 && sampleRate <= 1 : "sampleRage must be in range (0, 1]";
            this.sampleRate = sampleRate;
            return this;
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
        public ClsOpGradBoostHostConfig build() {
            return new ClsOpGradBoostHostConfig(this);
        }
    }
}
