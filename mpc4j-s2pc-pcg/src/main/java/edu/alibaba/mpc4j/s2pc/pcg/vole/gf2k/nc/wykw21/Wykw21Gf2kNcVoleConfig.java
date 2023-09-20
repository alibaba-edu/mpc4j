package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;

/**
 * WYKW21-GF2K-NC-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class Wykw21Gf2kNcVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kNcVoleConfig {
    /**
     * core GF2K-VOLE config
     */
    private final Gf2kCoreVoleConfig coreVoleConfig;
    /**
     * GF2K-MSP-VOLE config
     */
    private final Gf2kMspVoleConfig mspVoleConfig;

    private Wykw21Gf2kNcVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreVoleConfig, builder.mspVoleConfig);
        coreVoleConfig = builder.coreVoleConfig;
        mspVoleConfig = builder.mspVoleConfig;
    }

    public Gf2kCoreVoleConfig getCoreVoleConfig() {
        return coreVoleConfig;
    }

    public Gf2kMspVoleConfig getMspVoleConfig() {
        return mspVoleConfig;
    }

    @Override
    public Gf2kNcVoleFactory.Gf2kNcVoleType getPtoType() {
        return Gf2kNcVoleFactory.Gf2kNcVoleType.WYKW21;
    }

    @Override
    public int maxNum() {
        return 1 << Wykw21Gf2kNcVolePtoDesc.MAX_LOG_N;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21Gf2kNcVoleConfig> {
        /**
         * core COT config
         */
        private Gf2kCoreVoleConfig coreVoleConfig;
        /**
         * MSP-COT config
         */
        private Gf2kMspVoleConfig mspVoleConfig;

        public Builder(SecurityModel securityModel) {
            coreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(securityModel);
            mspVoleConfig = Gf2kMspVoleFactory.createDefaultConfig(securityModel);
        }

        public Builder setCoreVoleConfig(Gf2kCoreVoleConfig coreVoleConfig) {
            this.coreVoleConfig = coreVoleConfig;
            return this;
        }

        public Builder setMspVoleConfig(Gf2kMspVoleConfig mspVoleConfig) {
            this.mspVoleConfig = mspVoleConfig;
            return this;
        }

        @Override
        public Wykw21Gf2kNcVoleConfig build() {
            return new Wykw21Gf2kNcVoleConfig(this);
        }
    }
}
