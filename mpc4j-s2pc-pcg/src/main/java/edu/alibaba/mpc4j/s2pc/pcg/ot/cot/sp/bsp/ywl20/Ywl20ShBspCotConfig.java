package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotFactory.BspCotType;

/**
 * semi-honest YWL20-BSP-COT config.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotConfig extends AbstractMultiPartyPtoConfig implements BspCotConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfConfig bpRdpprfConfig;

    private Ywl20ShBspCotConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.bpRdpprfConfig);
        coreCotConfig = builder.coreCotConfig;
        bpRdpprfConfig = builder.bpRdpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public BpRdpprfConfig getBpDpprfConfig() {
        return bpRdpprfConfig;
    }

    @Override
    public BspCotType getPtoType() {
        return BspCotFactory.BspCotType.YWL20_SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20ShBspCotConfig> {
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;
        /**
         * BP-DPPRF
         */
        private BpRdpprfConfig bpRdpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bpRdpprfConfig = BpRdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setBpDpprfConfig(BpRdpprfConfig bpRdpprfConfig) {
            this.bpRdpprfConfig = bpRdpprfConfig;
            return this;
        }

        @Override
        public Ywl20ShBspCotConfig build() {
            return new Ywl20ShBspCotConfig(this);
        }
    }
}
