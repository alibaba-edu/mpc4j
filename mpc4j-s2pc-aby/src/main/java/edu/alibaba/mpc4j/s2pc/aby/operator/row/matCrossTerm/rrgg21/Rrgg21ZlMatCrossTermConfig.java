package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.ZlMatCrossTermConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.ZlMatCrossTermFactory;

/**
 * RRGG21 Zl Matrix Cross Term Multiplication Config.
 *
 * @author Liqiang Peng
 * @date 2024/6/12
 */
public class Rrgg21ZlMatCrossTermConfig extends AbstractMultiPartyPtoConfig implements ZlMatCrossTermConfig {
    /**
     * zl cross term multiplication config
     */
    private final ZlCrossTermConfig crossTermConfig;

    private Rrgg21ZlMatCrossTermConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.crossTermConfig);
        crossTermConfig = builder.crossTermConfig;
    }

    @Override
    public ZlMatCrossTermFactory.ZlMatCrossTermType getPtoType() {
        return ZlMatCrossTermFactory.ZlMatCrossTermType.RRGG21;
    }

    public ZlCrossTermConfig getCrossTermConfig() {
        return crossTermConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrgg21ZlMatCrossTermConfig> {
        /**
         * zl cross term multiplication config
         */
        private ZlCrossTermConfig crossTermConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            crossTermConfig = ZlCrossTermFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setCrossTermConfig(ZlCrossTermConfig crossTermConfig) {
            this.crossTermConfig = crossTermConfig;
            return this;
        }

        @Override
        public Rrgg21ZlMatCrossTermConfig build() {
            return new Rrgg21ZlMatCrossTermConfig(this);
        }
    }
}
