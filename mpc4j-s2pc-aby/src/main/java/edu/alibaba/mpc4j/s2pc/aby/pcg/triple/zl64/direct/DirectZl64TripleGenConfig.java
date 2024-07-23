package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory.Zl64TripleGenType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * direct Zl64 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
public class DirectZl64TripleGenConfig extends AbstractMultiPartyPtoConfig implements Zl64TripleGenConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;

    private DirectZl64TripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public Zl64TripleGenType getPtoType() {
        return Zl64TripleGenType.DIRECT_COT;
    }

    @Override
    public int defaultRoundNum(int l) {
        return (int) Math.floor((double) (1 << 22) / l);
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectZl64TripleGenConfig> {
        /**
         * core COT
         */
        private final CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public DirectZl64TripleGenConfig build() {
            return new DirectZl64TripleGenConfig(this);
        }
    }
}
