package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory;

/**
 * cache 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class CacheLnotConfig implements LnotConfig {
    /**
     * no-choice LNOT config
     */
    private final NcLnotConfig ncLnotConfig;
    /**
     * pre-compute LNOT config
     */
    private final PreLnotConfig preLnotConfig;

    private CacheLnotConfig(Builder builder) {
        // two environments must be the same
        assert builder.ncLnotConfig.getEnvType().equals(builder.preLnotConfig.getEnvType());
        ncLnotConfig = builder.ncLnotConfig;
        preLnotConfig = builder.preLnotConfig;
    }

    public NcLnotConfig getNcLnotConfig() {
        return ncLnotConfig;
    }

    public PreLnotConfig getPreLnotConfig() {
        return preLnotConfig;
    }

    @Override
    public LnotFactory.LnotType getPtoType() {
        return LnotFactory.LnotType.CACHE;
    }

    @Override
    public int maxBaseNum() {
        return ncLnotConfig.maxNum();
    }

    @Override
    public void setEnvType(EnvType envType) {
        ncLnotConfig.setEnvType(envType);
        preLnotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return ncLnotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (ncLnotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = ncLnotConfig.getSecurityModel();
        }
        if (preLnotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = preLnotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheLnotConfig> {
        /**
         * no-choice LNOT config
         */
        private NcLnotConfig ncLnotConfig;
        /**
         * precompute LNOT config
         */
        private PreLnotConfig preLnotConfig;

        public Builder(SecurityModel securityModel) {
            ncLnotConfig = NcLnotFactory.createDefaultConfig(securityModel);
            preLnotConfig = PreLnotFactory.createDefaultConfig(securityModel);
        }

        public Builder setNcLnotConfig(NcLnotConfig ncCotConfig) {
            this.ncLnotConfig = ncCotConfig;
            return this;
        }

        public Builder setPreLnotConfig(PreLnotConfig preCotConfig) {
            this.preLnotConfig = preCotConfig;
            return this;
        }

        @Override
        public CacheLnotConfig build() {
            return new CacheLnotConfig(this);
        }
    }
}
