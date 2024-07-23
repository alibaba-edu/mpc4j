package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;

/**
 * Direct Z2 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public class DirectZ2TripleGenConfig extends AbstractMultiPartyPtoConfig implements Z2TripleGenConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;

    private DirectZ2TripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public Z2TripleGenType getPtoType() {
        return Z2TripleGenType.DIRECT_COT;
    }

    @Override
    public int defaultRoundNum() {
        return 1 << 22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectZ2TripleGenConfig> {
        /**
         * COT
         */
        private final CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public DirectZ2TripleGenConfig build() {
            return new DirectZ2TripleGenConfig(this);
        }
    }
}
