package edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;

/**
 * RRK+20 Zl Greater Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlGreaterConfig extends AbstractMultiPartyPtoConfig implements ZlGreaterConfig {
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Zl MUX config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * Zl DReLU config.
     */
    private final ZlDreluConfig zlDreluConfig;

    private Rrk20ZlGreaterConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlcConfig, builder.zlMuxConfig, builder.zlDreluConfig);
        zlcConfig = builder.zlcConfig;
        zlMuxConfig = builder.zlMuxConfig;
        zlDreluConfig = builder.zlDreluConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public ZlDreluConfig getZlDreluConfig() {
        return zlDreluConfig;
    }

    @Override
    public ZlGreaterFactory.ZlGreaterType getPtoType() {
        return ZlGreaterFactory.ZlGreaterType.RRK20;
    }

    @Override
    public Zl getZl() {
        return zlcConfig.getZl();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlGreaterConfig> {
        /**
         * Zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * Zl MUX config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * Zl DReLU config.
         */
        private final ZlDreluConfig zlDreluConfig;

        public Builder(Zl zl) {
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            zlDreluConfig = ZlDreluFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        @Override
        public Rrk20ZlGreaterConfig build() {
            return new Rrk20ZlGreaterConfig(this);
        }
    }
}
