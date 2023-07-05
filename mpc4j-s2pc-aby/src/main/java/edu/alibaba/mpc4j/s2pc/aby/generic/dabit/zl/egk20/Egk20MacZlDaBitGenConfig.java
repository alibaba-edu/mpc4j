package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.ZlDaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.ZlDaBitGenFactory;

/**
 * EGK+20 Zl (MAC) daBit generation config.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class Egk20MacZlDaBitGenConfig extends AbstractMultiPartyPtoConfig implements ZlDaBitGenConfig {
    /**
     * Zl circuit config
     */
    private final ZlcConfig zlcConfig;
    /**
     * Z2 circuit config
     */
    private final Z2cConfig z2cConfig;

    private Egk20MacZlDaBitGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlcConfig, builder.z2cConfig);
        zlcConfig = builder.zlcConfig;
        z2cConfig = builder.z2cConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlDaBitGenFactory.ZlDaBitGenType getPtoType() {
        return ZlDaBitGenFactory.ZlDaBitGenType.EGK20_MAC;
    }

    @Override
    public Zl getZl() {
        return zlcConfig.getZl();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Egk20MacZlDaBitGenConfig> {
        /**
         * Zl circuit config
         */
        private ZlcConfig zlcConfig;
        /**
         * Z2 circuit config
         */
        private Z2cConfig z2cConfig;

        public Builder(SecurityModel securityModel, Zl zl, boolean silent) {
            zlcConfig = ZlcFactory.createDefaultConfig(securityModel, zl);
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setZlcConfig(ZlcConfig zlcConfig) {
            this.zlcConfig = zlcConfig;
            return this;
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public Egk20MacZlDaBitGenConfig build() {
            return new Egk20MacZlDaBitGenConfig(this);
        }
    }
}
