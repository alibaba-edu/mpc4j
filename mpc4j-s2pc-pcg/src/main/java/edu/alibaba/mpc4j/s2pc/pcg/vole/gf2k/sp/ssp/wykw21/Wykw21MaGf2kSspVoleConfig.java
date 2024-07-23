package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory.Gf2kSspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleConfig;

/**
 * malicious WYKW21-SSP-GF2K-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Wykw21MaGf2kSspVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kSspVoleConfig {
    /**
     * core GF2K-VOLE config
     */
    private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
    /**
     * SP-DPPRF config
     */
    private final SpRdpprfConfig spRdpprfConfig;

    private Wykw21MaGf2kSspVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.gf2kCoreVoleConfig, builder.spRdpprfConfig);
        gf2kCoreVoleConfig = builder.gf2kCoreVoleConfig;
        spRdpprfConfig = builder.spRdpprfConfig;
    }

    public Gf2kCoreVoleConfig getGf2kCoreVoleConfig() {
        return gf2kCoreVoleConfig;
    }

    public SpRdpprfConfig getSpDpprfConfig() {
        return spRdpprfConfig;
    }

    @Override
    public Gf2kSspVoleType getPtoType() {
        return Gf2kSspVoleFactory.Gf2kSspVoleType.WYKW21_MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21MaGf2kSspVoleConfig> {
        /**
         * core GF2K-VOLE config
         */
        private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
        /**
         * SP-DPPRF config
         */
        private final SpRdpprfConfig spRdpprfConfig;

        public Builder() {
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            spRdpprfConfig = SpRdpprfFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        @Override
        public Wykw21MaGf2kSspVoleConfig build() {
            return new Wykw21MaGf2kSspVoleConfig(this);
        }
    }
}
