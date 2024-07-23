package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory.SstType;

/**
 * CGP20-SST config.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class Cgp20SstConfig extends AbstractMultiPartyPtoConfig implements SstConfig {
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfConfig bpRdpprfConfig;

    private Cgp20SstConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bpRdpprfConfig);
        bpRdpprfConfig = builder.bpRdpprfConfig;
    }

    public BpRdpprfConfig getBpRdpprfConfig() {
        return bpRdpprfConfig;
    }

    @Override
    public SstType getPtoType() {
        return SstType.CGP20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgp20SstConfig> {
        /**
         * BP-RDPPRF
         */
        private BpRdpprfConfig bpRdpprfConfig;

        public Builder() {
            bpRdpprfConfig = BpRdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBpRdpprfConfig(BpRdpprfConfig bpRdpprfConfig) {
            this.bpRdpprfConfig = bpRdpprfConfig;
            return this;
        }

        @Override
        public Cgp20SstConfig build() {
            return new Cgp20SstConfig(this);
        }
    }
}
