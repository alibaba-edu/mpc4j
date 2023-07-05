package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory;

/**
 * direct no-choice 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class DirectNcLnotConfig extends AbstractMultiPartyPtoConfig implements NcLnotConfig {
    /**
     * LCOT config
     */
    private final LcotConfig lcotConfig;

    private DirectNcLnotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    @Override
    public NcLnotFactory.NcLnotType getPtoType() {
        return NcLnotFactory.NcLnotType.DIRECT;
    }

    @Override
    public int maxNum() {
        // In theory, LCOT can support arbitrary num. Here we limit the max num in case of memory exception.
        return 1 << 24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectNcLnotConfig> {
        /**
         * LCOT config
         */
        private LcotConfig lcotConfig;

        public Builder(SecurityModel securityModel) {
            lcotConfig = LcotFactory.createDefaultConfig(securityModel);
        }

        public Builder setLcotConfig(LcotConfig lcotConfig) {
            this.lcotConfig = lcotConfig;
            return this;
        }

        @Override
        public DirectNcLnotConfig build() {
            return new DirectNcLnotConfig(this);
        }
    }
}
