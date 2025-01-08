package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory.PstType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Partial share translate config, using the fixed Lll24Bst
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public class Lll24PstConfig extends AbstractMultiPartyPtoConfig implements PstConfig {
    /**
     * BP-CDPPRF
     */
    private final Lll24BstConfig bstConfig;
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Lll24PstConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bstConfig, builder.cotConfig);
        bstConfig = builder.bstConfig;
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public Lll24BstConfig getBstConfig() {
        return bstConfig;
    }

    @Override
    public PstType getPtoType() {
        return PstType.LLL24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lll24PstConfig> {
        /**
         * BP-CDPPRF
         */
        private Lll24BstConfig bstConfig;
        /**
         * COT config
         */
        private final CotConfig cotConfig;

        public Builder(boolean silent) {
            bstConfig = new Lll24BstConfig.Builder().build();
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setBstConfig(Lll24BstConfig bstConfig) {
            this.bstConfig = bstConfig;
            return this;
        }

        @Override
        public Lll24PstConfig build() {
            return new Lll24PstConfig(this);
        }
    }
}
