package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * direct 1-out-of-n (with n = 2^l) config.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class DirectLnotConfig extends AbstractMultiPartyPtoConfig implements LnotConfig {
    /**
     * 1-out-of-2^l COT config
     */
    private final LcotConfig lcotConfig;

    private DirectLnotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    @Override
    public LnotFactory.LnotType getPtoType() {
        return LnotFactory.LnotType.DIRECT;
    }

    @Override
    public int maxBaseNum() {
        // in theory, 1-out-of-2^l COT can support arbitrary number of COTs. Here we also provide some limitations.
        return 1 << 24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectLnotConfig> {
        /**
         * 1-out-of-2^l COT config
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
        public DirectLnotConfig build() {
            return new DirectLnotConfig(this);
        }
    }
}
