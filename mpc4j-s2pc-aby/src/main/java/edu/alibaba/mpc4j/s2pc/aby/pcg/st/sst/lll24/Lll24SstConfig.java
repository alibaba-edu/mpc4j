package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory.SstType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;

/**
 * LLL24-SST config.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Lll24SstConfig extends AbstractMultiPartyPtoConfig implements SstConfig {
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfConfig bpCdpprfConfig;

    private Lll24SstConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bpCdpprfConfig);
        bpCdpprfConfig = builder.bpCdpprfConfig;
    }

    public BpCdpprfConfig getBpCdpprfConfig() {
        return bpCdpprfConfig;
    }

    @Override
    public SstType getPtoType() {
        return SstType.LLL24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lll24SstConfig> {
        /**
         * BP-CDPPRF
         */
        private BpCdpprfConfig bpCdpprfConfig;

        public Builder() {
            bpCdpprfConfig = BpCdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBpRdpprfConfig(BpCdpprfConfig bpCdpprfConfig) {
            this.bpCdpprfConfig = bpCdpprfConfig;
            return this;
        }

        @Override
        public Lll24SstConfig build() {
            return new Lll24SstConfig(this);
        }
    }
}
