package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotFactory.BspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * GYW23-BSP-COT config.
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
public class Gyw23BspCotConfig extends AbstractMultiPartyPtoConfig implements BspCotConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * pre-compute COT
     */
    private final PreCotConfig preCotConfig;
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfConfig bpCdpprfConfig;

    private Gyw23BspCotConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.preCotConfig, builder.bpCdpprfConfig);
        coreCotConfig = builder.coreCotConfig;
        preCotConfig = builder.preCotConfig;
        bpCdpprfConfig = builder.bpCdpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public PreCotConfig getPreCotConfig() {
        return preCotConfig;
    }

    public BpCdpprfConfig getBpCdpprfConfig() {
        return bpCdpprfConfig;
    }

    @Override
    public BspCotType getPtoType() {
        return BspCotType.GYW23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gyw23BspCotConfig> {
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;
        /**
         * pre-compute COT
         */
        private PreCotConfig preCotConfig;
        /**
         * BP-CDPPRF
         */
        private BpCdpprfConfig bpCdpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            preCotConfig = PreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bpCdpprfConfig = BpCdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setPreCotConfig(PreCotConfig preCotConfig) {
            this.preCotConfig = preCotConfig;
            return this;
        }

        public Builder setBpCdpprfConfig(BpCdpprfConfig bpCdpprfConfig) {
            this.bpCdpprfConfig = bpCdpprfConfig;
            return this;
        }

        @Override
        public Gyw23BspCotConfig build() {
            return new Gyw23BspCotConfig(this);
        }
    }
}
