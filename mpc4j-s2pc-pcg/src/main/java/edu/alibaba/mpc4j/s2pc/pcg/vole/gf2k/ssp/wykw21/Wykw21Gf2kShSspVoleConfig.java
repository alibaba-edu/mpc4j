package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleFactory;

/**
 * WYKW21-SSP-GF2K-VOLE (semi-honest) config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Wykw21Gf2kShSspVoleConfig implements Gf2kSspVoleConfig {
    /**
     * GF2K core VOLE config
     */
    private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
    /**
     * single-point DPPRF config
     */
    private final SpDpprfConfig spDpprfConfig;

    private Wykw21Gf2kShSspVoleConfig(Builder builder) {
        assert builder.spDpprfConfig.getEnvType().equals(builder.gf2kCoreVoleConfig.getEnvType());
        gf2kCoreVoleConfig = builder.gf2kCoreVoleConfig;
        spDpprfConfig = builder.spDpprfConfig;
    }

    public Gf2kCoreVoleConfig getGf2kCoreVoleConfig() {
        return gf2kCoreVoleConfig;
    }

    public SpDpprfConfig getSpDpprfConfig() {
        return spDpprfConfig;
    }

    @Override
    public Gf2kSspVoleFactory.Gf2kSspVoleType getPtoType() {
        return Gf2kSspVoleFactory.Gf2kSspVoleType.WYKW21_SEMI_HONEST;
    }

    @Override
    public void setEnvType(EnvType envType) {
        gf2kCoreVoleConfig.setEnvType(envType);
        spDpprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return gf2kCoreVoleConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (gf2kCoreVoleConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = gf2kCoreVoleConfig.getSecurityModel();
        }
        if (spDpprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = spDpprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21Gf2kShSspVoleConfig> {
        /**
         * GF2K core VOLE config
         */
        private Gf2kCoreVoleConfig gf2kCoreVoleConfig;
        /**
         * single-point DPPRF config
         */
        private SpDpprfConfig spDpprfConfig;

        public Builder() {
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            spDpprfConfig = SpDpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setGf2kCoreVoleConfig(Gf2kCoreVoleConfig gf2kCoreVoleConfig) {
            this.gf2kCoreVoleConfig = gf2kCoreVoleConfig;
            return this;
        }

        public Builder setSpDpprfConfig(SpDpprfConfig spDpprfConfig) {
            this.spDpprfConfig = spDpprfConfig;
            return this;
        }

        @Override
        public Wykw21Gf2kShSspVoleConfig build() {
            return new Wykw21Gf2kShSspVoleConfig(this);
        }
    }
}
