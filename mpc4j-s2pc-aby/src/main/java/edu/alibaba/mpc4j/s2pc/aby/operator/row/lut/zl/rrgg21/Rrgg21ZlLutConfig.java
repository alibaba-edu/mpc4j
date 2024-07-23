package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.ZlLutConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.ZlLutFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * RRGG21 Zl lookup table protocol config.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlLutConfig extends AbstractMultiPartyPtoConfig implements ZlLutConfig {
    /**
     * 1-out-of-n (with n = 2^l) OT config
     */
    private final LnotConfig lnotConfig;

    private Rrgg21ZlLutConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lnotConfig);
        lnotConfig = builder.lnotConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    @Override
    public ZlLutFactory.ZlLutType getPtoType() {
        return ZlLutFactory.ZlLutType.RRGG21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrgg21ZlLutConfig> {
        /**
         * 1-out-of-n (with n = 2^l) OT config
         */
        private LnotConfig lnotConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            lnotConfig = LnotFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setLcotConfig(LnotConfig lnotConfig) {
            this.lnotConfig = lnotConfig;
            return this;
        }

        @Override
        public Rrgg21ZlLutConfig build() {
            return new Rrgg21ZlLutConfig(this);
        }
    }
}
