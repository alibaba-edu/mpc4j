package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.ZlMax2Config;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.ZlMax2Factory.ZlMax2Type;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;

/**
 * RRK+20 Zl Max2 Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMax2Config extends AbstractMultiPartyPtoConfig implements ZlMax2Config {
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

    private Rrk20ZlMax2Config(Builder builder) {
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
    public ZlMax2Type getPtoType() {
        return ZlMax2Type.RRK20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlMax2Config> {
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

        public Builder(SecurityModel securityModel, boolean silent) {
            zlcConfig = ZlcFactory.createDefaultConfig(securityModel, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(securityModel, silent);
            zlDreluConfig = ZlDreluFactory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public Rrk20ZlMax2Config build() {
            return new Rrk20ZlMax2Config(this);
        }
    }
}
