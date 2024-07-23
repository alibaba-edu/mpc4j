package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory.BpCdpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * GYW23-BP-CDPPRF config.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class Gyw23BpCdpprfConfig extends AbstractMultiPartyPtoConfig implements BpCdpprfConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * pre-compute COT
     */
    private final PreCotConfig preCotConfig;

    private Gyw23BpCdpprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.preCotConfig);
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
    public BpCdpprfType getPtoType() {
        return BpCdpprfType.GYW23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw23BpCdpprfConfig> {
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;
        /**
         * pre-compute COT
         */
        private PreCotConfig preCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            preCotConfig = PreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
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
        public Gyw23BpCdpprfConfig build() {
            return new Gyw23BpCdpprfConfig(this);
        }
    }
}
