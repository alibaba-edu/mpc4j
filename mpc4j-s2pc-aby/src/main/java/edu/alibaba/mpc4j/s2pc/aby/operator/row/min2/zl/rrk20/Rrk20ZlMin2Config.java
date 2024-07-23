package edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Config;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Factory.ZlMin2Type;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;

/**
 * RRK+20 Zl Min2 Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMin2Config extends AbstractMultiPartyPtoConfig implements ZlMin2Config {
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
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;

    private Rrk20ZlMin2Config(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlcConfig, builder.zlMuxConfig, builder.zlDreluConfig);
        zlcConfig = builder.zlcConfig;
        zlMuxConfig = builder.zlMuxConfig;
        zlDreluConfig = builder.zlDreluConfig;
        z2cConfig = builder.z2cConfig;
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

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlMin2Type getPtoType() {
        return ZlMin2Type.RRK20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlMin2Config> {
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
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            zlcConfig = ZlcFactory.createDefaultConfig(securityModel, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(securityModel, silent);
            zlDreluConfig = ZlDreluFactory.createDefaultConfig(securityModel, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public Rrk20ZlMin2Config build() {
            return new Rrk20ZlMin2Config(this);
        }
    }
}
