package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * cache COT config.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CacheCotConfig extends AbstractMultiPartyPtoConfig implements CotConfig {
    /**
     * no-choice COT config
     */
    private final NcCotConfig ncCotConfig;
    /**
     * pre-compute COT config
     */
    private final PreCotConfig preCotConfig;

    private CacheCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ncCotConfig, builder.preCotConfig);
        ncCotConfig = builder.ncCotConfig;
        preCotConfig = builder.preCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    public PreCotConfig getPreCotConfig() {
        return preCotConfig;
    }

    @Override
    public CotFactory.CotType getPtoType() {
        return CotFactory.CotType.CACHE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheCotConfig> {
        /**
         * no-choice COT config
         */
        private NcCotConfig ncCotConfig;
        /**
         * precompute COT config
         */
        private PreCotConfig preCotConfig;

        public Builder(SecurityModel securityModel) {
            ncCotConfig = NcCotFactory.createDefaultConfig(securityModel, true);
            preCotConfig = PreCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.ncCotConfig = ncCotConfig;
            return this;
        }

        public Builder setPreCotConfig(PreCotConfig preCotConfig) {
            this.preCotConfig = preCotConfig;
            return this;
        }

        @Override
        public CacheCotConfig build() {
            return new CacheCotConfig(this);
        }
    }
}
