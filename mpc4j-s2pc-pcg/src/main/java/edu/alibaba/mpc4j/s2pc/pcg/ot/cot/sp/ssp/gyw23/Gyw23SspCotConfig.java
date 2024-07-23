package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * GYW23-SSP-COT config.
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
public class Gyw23SspCotConfig extends AbstractMultiPartyPtoConfig implements SspCotConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * pre-compute COT
     */
    private final PreCotConfig preCotConfig;
    /**
     * SP-CDPPRF
     */
    private final SpCdpprfConfig spCdpprfConfig;

    private Gyw23SspCotConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.preCotConfig, builder.spCdpprfConfig);
        coreCotConfig = builder.coreCotConfig;
        preCotConfig = builder.preCotConfig;
        spCdpprfConfig = builder.spCdpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public PreCotConfig getPreCotConfig() {
        return preCotConfig;
    }

    public SpCdpprfConfig getSpCdpprfConfig() {
        return spCdpprfConfig;
    }

    @Override
    public SspCotType getPtoType() {
        return SspCotType.GYW23_SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw23SspCotConfig> {
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;
        /**
         * pre-compute COT
         */
        private PreCotConfig preCotConfig;
        /**
         * SP-CDPPRF
         */
        private SpCdpprfConfig spCdpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            preCotConfig = PreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            spCdpprfConfig = SpCdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setPreCotConfig(PreCotConfig preCotConfig) {
            this.preCotConfig = preCotConfig;
            return this;
        }

        public Builder setSpCdpprfConfig(SpCdpprfConfig spCdpprfConfig) {
            this.spCdpprfConfig = spCdpprfConfig;
            return this;
        }

        @Override
        public Gyw23SspCotConfig build() {
            return new Gyw23SspCotConfig(this);
        }
    }

}
