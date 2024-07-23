package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;

/**
 * LCOT Z2 triple generation config.
 *
 * @author Liqiang Peng
 * @date 2024/5/27
 */
public class LcotZ2TripleGenConfig extends AbstractMultiPartyPtoConfig implements Z2TripleGenConfig {
    /**
     * LCOT
     */
    private final LcotConfig lcotConfig;

    private LcotZ2TripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    @Override
    public Z2TripleGenType getPtoType() {
        return Z2TripleGenType.LCOT;
    }

    @Override
    public int defaultRoundNum() {
        return 1 << 22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<LcotZ2TripleGenConfig> {
        /**
         * LCOT
         */
        private final LcotConfig lcotConfig;

        public Builder() {
            lcotConfig = LcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public LcotZ2TripleGenConfig build() {
            return new LcotZ2TripleGenConfig(this);
        }
    }
}
