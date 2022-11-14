package edu.alibaba.mpc4j.s2pc.pso.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;

/**
 * GMR21-mqRPMT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtConfig implements MqRpmtConfig {
    /**
     * 布谷鸟哈希所用OPRF协议配置项
     */
    private final OprfConfig cuckooHashOprfConfig;
    /**
     * PEQT所用OPRF协议配置项
     */
    private final OprfConfig peqtOprfConfig;
    /**
     * OSN协议配置项
     */
    private final OsnConfig osnConfig;
    /**
     * OKVS类型
     */
    private final OkvsType okvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Gmr21MqRpmtConfig(Builder builder) {
        // 协议的环境类型必须相同
        assert builder.cuckooHashOprfConfig.getEnvType().equals(builder.peqtOprfConfig.getEnvType());
        assert builder.cuckooHashOprfConfig.getEnvType().equals(builder.osnConfig.getEnvType());
        cuckooHashOprfConfig = builder.cuckooHashOprfConfig;
        peqtOprfConfig = builder.peqtOprfConfig;
        osnConfig = builder.osnConfig;
        okvsType = builder.okvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return MqRpmtFactory.MqRpmtType.GMR21;
    }

    public OprfConfig getCuckooHashOprfConfig() {
        return cuckooHashOprfConfig;
    }

    public OprfConfig getPeqtOprfConfig() {
        return peqtOprfConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public OkvsType getOkvsType() {
        return okvsType;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public void setEnvType(EnvType envType) {
        cuckooHashOprfConfig.setEnvType(envType);
        peqtOprfConfig.setEnvType(envType);
        osnConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cuckooHashOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cuckooHashOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cuckooHashOprfConfig.getSecurityModel();
        }
        if (peqtOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = peqtOprfConfig.getSecurityModel();
        }
        if (osnConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = osnConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21MqRpmtConfig> {
        /**
         * 布谷鸟哈希所用OPRF协议配置项
         */
        private OprfConfig cuckooHashOprfConfig;
        /**
         * PEQT所用OPRF协议配置项
         */
        private OprfConfig peqtOprfConfig;
        /**
         * OSN协议配置项
         */
        private OsnConfig osnConfig;
        /**
         * OKVS类型
         */
        private OkvsType okvsType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            cuckooHashOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = OkvsType.MEGA_BIN;
            // GMR21源代码使用普通布谷鸟哈希实现无贮存区布谷鸟哈希的功能，这样通信量可以更小一点
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setCuckooHashOprfConfig(OprfConfig cuckooHashOprfConfig) {
            this.cuckooHashOprfConfig = cuckooHashOprfConfig;
            return this;
        }

        public Builder setPeqtOprfConfig(OprfConfig peqtOprfConfig) {
            this.peqtOprfConfig = peqtOprfConfig;
            return this;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        public Builder setOkvsType(OkvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Gmr21MqRpmtConfig build() {
            return new Gmr21MqRpmtConfig(this);
        }
    }
}
