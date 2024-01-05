package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * GP23 Zl Truncation Config.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class Gp23ZlTruncConfig extends AbstractMultiPartyPtoConfig implements ZlTruncConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * cot protocol config.
     */
    private final CotConfig cotConfig;

    private Gp23ZlTruncConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.cotConfig);
        z2cConfig = builder.z2cConfig;
        cotConfig = builder.cotConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlTruncFactory.ZlTruncType getPtoType() {
        return ZlTruncFactory.ZlTruncType.GP23;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gp23ZlTruncConfig> {
        /**
         * Z2 circuit config.
         */
        private Z2cConfig z2cConfig;
        /**
         * cot protocol config.
         */
        private CotConfig cotConfig;

        public Builder(boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Gp23ZlTruncConfig build() {
            return new Gp23ZlTruncConfig(this);
        }
    }
}
