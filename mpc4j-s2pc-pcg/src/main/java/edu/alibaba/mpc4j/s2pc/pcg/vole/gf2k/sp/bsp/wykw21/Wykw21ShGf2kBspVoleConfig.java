package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory.Gf2kBspVoleType;

/**
 * semi-honest WYKW21-BSP-GF2K-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Wykw21ShGf2kBspVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kBspVoleConfig {
    /**
     * core GF2K-VOLE config
     */
    private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
    /**
     * BP-DPPRF config
     */
    private final BpRdpprfConfig bpRdpprfConfig;

    private Wykw21ShGf2kBspVoleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.gf2kCoreVoleConfig, builder.bpRdpprfConfig);
        gf2kCoreVoleConfig = builder.gf2kCoreVoleConfig;
        bpRdpprfConfig = builder.bpRdpprfConfig;
    }

    public Gf2kCoreVoleConfig getGf2kCoreVoleConfig() {
        return gf2kCoreVoleConfig;
    }

    public BpRdpprfConfig getBpDpprfConfig() {
        return bpRdpprfConfig;
    }

    @Override
    public Gf2kBspVoleType getPtoType() {
        return Gf2kBspVoleType.WYKW21_SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21ShGf2kBspVoleConfig> {
        /**
         * core GF2K-VOLE config
         */
        private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
        /**
         * BP-DPPRF config
         */
        private final BpRdpprfConfig bpRdpprfConfig;

        public Builder() {
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bpRdpprfConfig = BpRdpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Wykw21ShGf2kBspVoleConfig build() {
            return new Wykw21ShGf2kBspVoleConfig(this);
        }
    }
}
