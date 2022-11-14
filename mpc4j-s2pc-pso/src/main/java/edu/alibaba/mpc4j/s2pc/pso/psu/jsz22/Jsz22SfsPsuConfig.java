package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * JSZ22-SFS-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/03/18
 */
public class Jsz22SfsPsuConfig implements PsuConfig {
    /**
     * OPRF协议配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * OSN协议配置项
     */
    private final OsnConfig osnConfig;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Jsz22SfsPsuConfig(Builder builder) {
        // 协议的环境类型必须相同
        assert builder.oprfConfig.getEnvType().equals(builder.osnConfig.getEnvType());
        oprfConfig = builder.oprfConfig;
        osnConfig = builder.osnConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PsuFactory.PsuType getPtoType() {
        return PsuFactory.PsuType.JSZ22_SFS;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public void setEnvType(EnvType envType) {
        oprfConfig.setEnvType(envType);
        osnConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return oprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (oprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = oprfConfig.getSecurityModel();
        }
        if (osnConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = osnConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Jsz22SfsPsuConfig> {
        /**
         * OPRF协议配置项
         */
        private OprfConfig oprfConfig;
        /**
         * OSN协议配置项
         */
        private OsnConfig osnConfig;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            // 论文建议平衡场景下使用PSZ18的3哈希协议，非平衡场景下使用PSZ18的4哈希协议
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Jsz22SfsPsuConfig build() {
            return new Jsz22SfsPsuConfig(this);
        }
    }
}
