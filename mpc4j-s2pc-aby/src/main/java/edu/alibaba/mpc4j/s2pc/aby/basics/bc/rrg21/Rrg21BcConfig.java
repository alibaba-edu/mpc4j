package edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * RRG+21 Boolean circuit config.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Rrg21BcConfig implements BcConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Rrg21BcConfig(Builder builder) {
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public BcFactory.BcType getPtoType() {
        return BcFactory.BcType.RRG21;
    }

    @Override
    public void setEnvType(EnvType envType) {
        cotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrg21BcConfig> {
        /**
         * no-choice COT config
         */
        private CotConfig cotConfig;

        public Builder() {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Rrg21BcConfig build() {
            return new Rrg21BcConfig(this);
        }
    }
}
