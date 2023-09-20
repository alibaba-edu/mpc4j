package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;

/**
 * direct GF2K-NC-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class DirectGf2kNcVoleConfig extends AbstractMultiPartyPtoConfig implements Gf2kNcVoleConfig {
    /**
     * core GF2K-VOLE config
     */
    private final Gf2kCoreVoleConfig coreVoleConfig;

    private DirectGf2kNcVoleConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreVoleConfig);
        coreVoleConfig = builder.coreVoleConfig;
    }

    public Gf2kCoreVoleConfig getCoreVoleConfig() {
        return coreVoleConfig;
    }

    @Override
    public Gf2kNcVoleFactory.Gf2kNcVoleType getPtoType() {
        return Gf2kNcVoleFactory.Gf2kNcVoleType.DIRECT;
    }

    @Override
    public int maxNum() {
        // In theory, core COT can support arbitrary num. Here we limit the max num in case of memory exception.
        return 1 << 24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectGf2kNcVoleConfig> {
        /**
         * core GF2K-VOLE config
         */
        private Gf2kCoreVoleConfig coreVoleConfig;

        public Builder(SecurityModel securityModel) {
            coreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(securityModel);
        }

        public Builder setCoreVoleConfig(Gf2kCoreVoleConfig coreVoleConfig) {
            this.coreVoleConfig = coreVoleConfig;
            return this;
        }

        @Override
        public DirectGf2kNcVoleConfig build() {
            return new DirectGf2kNcVoleConfig(this);
        }
    }
}
