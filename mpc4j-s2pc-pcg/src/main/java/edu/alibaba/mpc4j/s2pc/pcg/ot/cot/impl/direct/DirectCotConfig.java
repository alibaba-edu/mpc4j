package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * direct COT config.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class DirectCotConfig extends AbstractMultiPartyPtoConfig implements CotConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    private DirectCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public CotFactory.CotType getPtoType() {
        return CotFactory.CotType.DIRECT;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectCotConfig> {
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
        public DirectCotConfig build() {
            return new DirectCotConfig(this);
        }
    }
}
