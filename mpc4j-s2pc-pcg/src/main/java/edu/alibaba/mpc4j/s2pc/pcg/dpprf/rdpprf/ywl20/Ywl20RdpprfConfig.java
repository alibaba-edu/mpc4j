package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.RdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * YWL20-RDPPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20RdpprfConfig implements RdpprfConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Ywl20RdpprfConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public DpprfFactory.DpprfType getPtoType() {
        return DpprfFactory.DpprfType.YWL20_RANDOM;
    }

    @Override
    public void setEnvType(EnvType envType) {
        coreCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return coreCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20RdpprfConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;

        public Builder(SecurityModel securityModel) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Ywl20RdpprfConfig build() {
            return new Ywl20RdpprfConfig(this);
        }
    }
}
