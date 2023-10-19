package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * GP23 Zl Corr Config.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class Gp23ZlCorrConfig extends AbstractMultiPartyPtoConfig implements ZlCorrConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * cot protocol config.
     */
    private final CotConfig cotConfig;

    private Gp23ZlCorrConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.cotConfig);
        z2cConfig = builder.z2cConfig;
        cotConfig = builder.cotConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlCorrFactory.ZlCorrType getPtoType() {
        return ZlCorrFactory.ZlCorrType.GP23;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gp23ZlCorrConfig> {
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * cot protocol config.
         */
        private final CotConfig cotConfig;

        public Builder(boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Gp23ZlCorrConfig build() {
            return new Gp23ZlCorrConfig(this);
        }
    }
}
