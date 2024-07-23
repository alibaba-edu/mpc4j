package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory.Gf2kSspVodeType;

/**
 * APRR24 GF2K-SSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Aprr24Gf2kSspVodeConfig extends AbstractMultiPartyPtoConfig implements Gf2kSspVodeConfig {
    /**
     * core GF2K-VODE config
     */
    private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;
    /**
     * SP-DPPRF config
     */
    private final SpRdpprfConfig spRdpprfConfig;

    private Aprr24Gf2kSspVodeConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.gf2kCoreVodeConfig, builder.spRdpprfConfig);
        gf2kCoreVodeConfig = builder.gf2kCoreVodeConfig;
        spRdpprfConfig = builder.spRdpprfConfig;
    }

    public Gf2kCoreVodeConfig getGf2kCoreVodeConfig() {
        return gf2kCoreVodeConfig;
    }

    public SpRdpprfConfig getSpDpprfConfig() {
        return spRdpprfConfig;
    }

    @Override
    public Gf2kSspVodeType getPtoType() {
        return Gf2kSspVodeType.APRR24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24Gf2kSspVodeConfig> {
        /**
         * core GF2K-VODE config
         */
        private final Gf2kCoreVodeConfig gf2kCoreVodeConfig;
        /**
         * SP-DPPRF config
         */
        private final SpRdpprfConfig spRdpprfConfig;

        public Builder() {
            gf2kCoreVodeConfig = Gf2kCoreVodeFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            spRdpprfConfig = SpRdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Aprr24Gf2kSspVodeConfig build() {
            return new Aprr24Gf2kSspVodeConfig(this);
        }
    }
}
