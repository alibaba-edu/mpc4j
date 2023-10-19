package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * RRK+20 Zl Corr Config.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class Rrk20ZlCorrConfig extends AbstractMultiPartyPtoConfig implements ZlCorrConfig {
    /**
     * Zl DReLU config
     */
    private final ZlDreluConfig zlDreluConfig;
    /**
     * 1-out-of-n (with n = 2^l) ot protocol config.
     */
    private final LnotConfig lnotConfig;

    private Rrk20ZlCorrConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlDreluConfig, builder.lnotConfig);
        zlDreluConfig = builder.zlDreluConfig;
        lnotConfig = builder.lnotConfig;
    }

    public ZlDreluConfig getZlDreluConfig() {
        return zlDreluConfig;
    }

    @Override
    public ZlCorrFactory.ZlCorrType getPtoType() {
        return ZlCorrFactory.ZlCorrType.RRK20;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlCorrConfig> {
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
        public Rrk20ZlCorrConfig build() {
            return new Rrk20ZlCorrConfig(this);
        }
    }
}
