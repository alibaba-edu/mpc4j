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
 * malicious YWL20-BSP-COT config.
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaBspCotConfig extends AbstractMultiPartyPtoConfig implements BspCotConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfConfig bpRdpprfConfig;

    private Ywl20MaBspCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig, builder.bpRdpprfConfig);
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
        return BspCotFactory.BspCotType.YWL20_MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20MaBspCotConfig> {
        /**
         * core COT
         */
        private CoreCotConfig coreCotConfig;
        /**
         * BP-DPPRF
         */
        private BpRdpprfConfig bpRdpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            bpRdpprfConfig = BpRdpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
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
        public Ywl20MaBspCotConfig build() {
            return new Ywl20MaBspCotConfig(this);
        }
    }
}
