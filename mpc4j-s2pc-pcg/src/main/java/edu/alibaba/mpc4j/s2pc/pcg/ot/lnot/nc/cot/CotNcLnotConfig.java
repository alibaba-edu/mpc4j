package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.cot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
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
public class CotNcLnotConfig extends AbstractMultiPartyPtoConfig implements NcLnotConfig {
    /**
     * no-choice COT config
     */
    private final NcCotConfig ncCotConfig;

    private CotNcLnotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ncCotConfig);
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
