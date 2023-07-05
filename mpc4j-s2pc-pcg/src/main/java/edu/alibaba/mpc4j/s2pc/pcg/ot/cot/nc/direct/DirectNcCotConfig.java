package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * direct no-choice COT config.
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class DirectNcCotConfig extends AbstractMultiPartyPtoConfig implements NcCotConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    private DirectNcCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public NcCotFactory.NcCotType getPtoType() {
        return NcCotFactory.NcCotType.DIRECT;
    }

    @Override
    public int maxNum() {
        // In theory, core COT can support arbitrary num. Here we limit the max num in case of memory exception.
        return 1 << 24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectNcCotConfig> {
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
        public DirectNcCotConfig build() {
            return new DirectNcCotConfig(this);
        }
    }
}
