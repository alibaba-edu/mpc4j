package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory.SpRdpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * YWL20-SP-RDPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Ywl20SpRdpprfConfig extends AbstractMultiPartyPtoConfig implements SpRdpprfConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * pre-compute COT config
     */
    private final PreCotConfig preCotConfig;

    private Ywl20SpRdpprfConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig, builder.preCotConfig);
        coreCotConfig = builder.coreCotConfig;
        preCotConfig = builder.preCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public PreCotConfig getPreCotConfig() {
        return preCotConfig;
    }

    @Override
    public SpRdpprfType getPtoType() {
        return SpRdpprfType.YWL20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20SpRdpprfConfig> {
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;
        /**
         * pre-compute COT config
         */
        private PreCotConfig preCotConfig;

        public Builder(SecurityModel securityModel) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(securityModel);
            preCotConfig = PreCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setPreCotConfig(PreCotConfig preCotConfig) {
            this.preCotConfig = preCotConfig;
            return this;
        }

        @Override
        public Ywl20SpRdpprfConfig build() {
            return new Ywl20SpRdpprfConfig(this);
        }
    }
}
