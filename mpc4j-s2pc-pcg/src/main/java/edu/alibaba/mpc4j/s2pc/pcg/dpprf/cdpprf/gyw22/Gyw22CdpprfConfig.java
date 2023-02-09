package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.gyw22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.CdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * GYW22-CDPPRF config.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class Gyw22CdpprfConfig implements CdpprfConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    private Gyw22CdpprfConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public DpprfFactory.DpprfType getPtoType() {
        return DpprfFactory.DpprfType.GYW22_CORRELATED;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw22CdpprfConfig> {
        /**
         * core COT config
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
        public Gyw22CdpprfConfig build() {
            return new Gyw22CdpprfConfig(this);
        }
    }
}
