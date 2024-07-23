package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * RRGG21 Zl Cross Term Multiplication Config.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlCrossTermConfig extends AbstractMultiPartyPtoConfig implements ZlCrossTermConfig {
    /**
     * cot config
     */
    private final CotConfig cotConfig;

    private Rrgg21ZlCrossTermConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    @Override
    public ZlCrossTermFactory.ZlCrossTermType getPtoType() {
        return ZlCrossTermFactory.ZlCrossTermType.RRGG21;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrgg21ZlCrossTermConfig> {
        /**
         * cot config
         */
        private CotConfig cotConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Rrgg21ZlCrossTermConfig build() {
            return new Rrgg21ZlCrossTermConfig(this);
        }
    }
}
