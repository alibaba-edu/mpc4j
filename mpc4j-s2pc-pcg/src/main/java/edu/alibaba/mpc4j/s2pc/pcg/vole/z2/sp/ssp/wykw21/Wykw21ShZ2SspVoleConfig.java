package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleFactory;

/**
 * WYKW21-Z2-SSP-VOLE半诚实安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public class Wykw21ShZ2SspVoleConfig implements Z2SspVoleConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Wykw21ShZ2SspVoleConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public Z2SspVoleFactory.Z2SspVoleType getPtoType() {
        return Z2SspVoleFactory.Z2SspVoleType.WYKW21_SEMI_HONEST;
    }

    @Override
    public EnvType getEnvType() {
        return coreCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21ShZ2SspVoleConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Wykw21ShZ2SspVoleConfig build() {
            return new Wykw21ShZ2SspVoleConfig(this);
        }
    }
}
