package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory.Gf2kBspVoleType;

/**
 * malicious WYKW21-BSP-GF2K-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Wykw21MaGf2kBspVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kBspVoleConfig {
    /**
     * core GF2K-VOLE config
     */
    private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
    /**
     * BP-DPPRF config
     */
    private final BpRdpprfConfig bpRdpprfConfig;

    private Wykw21MaGf2kBspVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kCoreVoleConfig, builder.bpRdpprfConfig);
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
        return Gf2kBspVoleFactory.Gf2kBspVoleType.WYKW21_MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21MaGf2kBspVoleConfig> {
        /**
         * core GF2K-VOLE config
         */
        private Gf2kCoreVoleConfig gf2kCoreVoleConfig;
        /**
         * BP-DPPRF config
         */
        private BpRdpprfConfig bpRdpprfConfig;

        public Builder() {
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            bpRdpprfConfig = BpRdpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setGf2kCoreVoleConfig(Gf2kCoreVoleConfig gf2kCoreVoleConfig) {
            this.gf2kCoreVoleConfig = gf2kCoreVoleConfig;
            return this;
        }

        public Builder setSpDpprfConfig(BpRdpprfConfig bpRdpprfConfig) {
            this.bpRdpprfConfig = bpRdpprfConfig;
            return this;
        }

        @Override
        public Wykw21MaGf2kBspVoleConfig build() {
            return new Wykw21MaGf2kBspVoleConfig(this);
        }
    }
}
