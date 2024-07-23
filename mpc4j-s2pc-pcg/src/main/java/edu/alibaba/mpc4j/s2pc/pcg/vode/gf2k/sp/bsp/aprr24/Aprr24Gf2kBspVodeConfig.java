package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory.Gf2kBspVodeType;

/**
 * APRR24 GF2K-BSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Aprr24Gf2kBspVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kBspVodeConfig {
    /**
     * core GF2K-VODE config
     */
    private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;
    /**
     * BP-DPPRF config
     */
    private final BpRdpprfConfig bpRdpprfConfig;

    private Aprr24Gf2kBspVodeConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.gf2kCoreVodeConfig, builder.bpRdpprfConfig);
        gf2kCoreVodeConfig = builder.gf2kCoreVodeConfig;
        bpRdpprfConfig = builder.bpRdpprfConfig;
    }

    public Gf2kCoreVodeConfig getGf2kCoreVodeConfig() {
        return gf2kCoreVodeConfig;
    }

    public BpRdpprfConfig getBpDpprfConfig() {
        return bpRdpprfConfig;
    }

    @Override
    public Gf2kBspVodeType getPtoType() {
        return Gf2kBspVodeType.APRR24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24Gf2kBspVodeConfig> {
        /**
         * core GF2K-VODE config
         */
        private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;
        /**
         * BP-DPPRF config
         */
        private final BpRdpprfConfig bpRdpprfConfig;

        public Builder() {
            gf2kCoreVodeConfig = Gf2kCoreVodeFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bpRdpprfConfig = BpRdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Aprr24Gf2kBspVodeConfig build() {
            return new Aprr24Gf2kBspVodeConfig(this);
        }
    }
}
