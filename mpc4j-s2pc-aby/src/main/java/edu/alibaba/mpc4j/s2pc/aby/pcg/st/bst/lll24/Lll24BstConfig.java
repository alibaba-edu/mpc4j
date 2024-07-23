package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory.BstType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;

/**
 * Lll24-BST config.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Lll24BstConfig extends AbstractMultiPartyPtoConfig implements BstConfig {
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfConfig bpCdpprfConfig;

    private Lll24BstConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bpCdpprfConfig);
        bpCdpprfConfig = builder.bpCdpprfConfig;
    }

    public BpCdpprfConfig getBpCdpprfConfig() {
        return bpCdpprfConfig;
    }

    @Override
    public BstType getPtoType() {
        return BstType.LLL24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lll24BstConfig> {
        /**
         * BP-CDPPRF
         */
        private BpCdpprfConfig bpCdpprfConfig;

        public Builder() {
            bpCdpprfConfig = BpCdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBpCdpprfConfig(BpCdpprfConfig bpCdpprfConfig) {
            this.bpCdpprfConfig = bpCdpprfConfig;
            return this;
        }

        @Override
        public Lll24BstConfig build() {
            return new Lll24BstConfig(this);
        }
    }
}
