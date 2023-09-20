package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;

/**
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
    private final BpDpprfConfig bpDpprfConfig;

    private Wykw21MaGf2kBspVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kCoreVoleConfig, builder.bpDpprfConfig);
        gf2kCoreVoleConfig = builder.gf2kCoreVoleConfig;
        bpDpprfConfig = builder.bpDpprfConfig;
    }

    public Gf2kCoreVoleConfig getGf2kCoreVoleConfig() {
        return gf2kCoreVoleConfig;
    }

    public BpDpprfConfig getBpDpprfConfig() {
        return bpDpprfConfig;
    }

    @Override
    public Gf2kBspVoleFactory.Gf2kBspVoleType getPtoType() {
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
        private BpDpprfConfig bpDpprfConfig;

        public Builder() {
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            bpDpprfConfig = BpDpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setGf2kCoreVoleConfig(Gf2kCoreVoleConfig gf2kCoreVoleConfig) {
            this.gf2kCoreVoleConfig = gf2kCoreVoleConfig;
            return this;
        }

        public Builder setSpDpprfConfig(BpDpprfConfig bpDpprfConfig) {
            this.bpDpprfConfig = bpDpprfConfig;
            return this;
        }

        @Override
        public Wykw21MaGf2kBspVoleConfig build() {
            return new Wykw21MaGf2kBspVoleConfig(this);
        }
    }
}
