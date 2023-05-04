package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.cot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory;

/**
 * COT no-choice 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class CotNcLnotConfig implements NcLnotConfig {
    /**
     * no-choice COT config
     */
    private final NcCotConfig ncCotConfig;

    private CotNcLnotConfig(Builder builder) {
        ncCotConfig = builder.ncCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public NcLnotFactory.NcLnotType getPtoType() {
        return NcLnotFactory.NcLnotType.COT;
    }

    @Override
    public int maxNum() {
        // In theory, LCOT can support arbitrary num. Here we limit the max num in case of memory exception.
        return 1 << 24;
    }

    @Override
    public void setEnvType(EnvType envType) {
        ncCotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return ncCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (ncCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = ncCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CotNcLnotConfig> {
        /**
         * no-choice COT config
         */
        private NcCotConfig ncCotConfig;

        public Builder(SecurityModel securityModel) {
            ncCotConfig = NcCotFactory.createDefaultConfig(securityModel, true);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.ncCotConfig = ncCotConfig;
            return this;
        }

        @Override
        public CotNcLnotConfig build() {
            return new CotNcLnotConfig(this);
        }
    }
}
