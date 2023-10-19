package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * RRK+20 Zl Truncation Config.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class Rrk20ZlTruncConfig extends AbstractMultiPartyPtoConfig implements ZlTruncConfig {
    /**
     * Zl DReLU config
     */
    private final ZlDreluConfig zlDreluConfig;
    /**
     * 1-out-of-n (with n = 2^l) ot protocol config.
     */
    private final LnotConfig lnotConfig;

    private Rrk20ZlTruncConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlDreluConfig, builder.lnotConfig);
        zlDreluConfig = builder.zlDreluConfig;
        lnotConfig = builder.lnotConfig;
    }

    public ZlDreluConfig getZlDreluConfig() {
        return zlDreluConfig;
    }

    @Override
    public ZlTruncFactory.ZlTruncType getPtoType() {
        return ZlTruncFactory.ZlTruncType.RRK20;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlTruncConfig> {
        /**
         * Zl DReLU config
         */
        private final ZlDreluConfig zlDreluConfig;
        /**
         * 1-out-of-n (with n = 2^l) ot protocol config.
         */
        private final LnotConfig lnotConfig;


        public Builder(boolean silent) {
            zlDreluConfig = ZlDreluFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            if (silent) {
                lnotConfig = LnotFactory.createCacheConfig(SecurityModel.SEMI_HONEST);
            } else {
                lnotConfig = LnotFactory.createDirectConfig(SecurityModel.SEMI_HONEST);
            }
        }

        @Override
        public Rrk20ZlTruncConfig build() {
            return new Rrk20ZlTruncConfig(this);
        }
    }
}
