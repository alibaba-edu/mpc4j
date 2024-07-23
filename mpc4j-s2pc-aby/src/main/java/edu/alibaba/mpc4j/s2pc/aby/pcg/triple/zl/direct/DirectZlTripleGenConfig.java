package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory.ZlTripleGenType;

/**
 * DSZ15 OT-based Zl triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class DirectZlTripleGenConfig extends AbstractMultiPartyPtoConfig implements ZlTripleGenConfig {
    /**
     * core COT
     */
    private final CoreCotConfig coreCotConfig;

    private DirectZlTripleGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public ZlTripleGenType getPtoType() {
        return ZlTripleGenType.DIRECT_COT;
    }

    @Override
    public int defaultRoundNum(int l) {
        return (int) Math.floor((double) (1 << 22) / l);
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectZlTripleGenConfig> {
        /**
         * core COT
         */
        private final CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public DirectZlTripleGenConfig build() {
            return new DirectZlTripleGenConfig(this);
        }
    }
}
