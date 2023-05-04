package edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * Bea91 Boolean circuit config.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcConfig implements BcConfig {
    /**
     * Boolean triple generation config
     */
    private final Z2MtgConfig z2MtgConfig;

    private Bea91BcConfig(Builder builder) {
        z2MtgConfig = builder.z2MtgConfig;
    }

    public Z2MtgConfig getZ2MtgConfig() {
        return z2MtgConfig;
    }

    @Override
    public BcFactory.BcType getPtoType() {
        return BcFactory.BcType.Bea91;
    }

    @Override
    public void setEnvType(EnvType envType) {
        z2MtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return z2MtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (z2MtgConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = z2MtgConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91BcConfig> {
        /**
         * Boolean triple generation config
         */
        private Z2MtgConfig z2MtgConfig;

        public Builder() {
            z2MtgConfig = Z2MtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setZ2MtgConfig(Z2MtgConfig z2MtgConfig) {
            this.z2MtgConfig = z2MtgConfig;
            return this;
        }

        @Override
        public Bea91BcConfig build() {
            return new Bea91BcConfig(this);
        }
    }
}
