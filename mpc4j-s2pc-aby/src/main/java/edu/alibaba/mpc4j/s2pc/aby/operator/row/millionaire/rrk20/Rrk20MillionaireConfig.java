package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * RRK+20 Millionaire Protocol Config.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Rrk20MillionaireConfig extends AbstractMultiPartyPtoConfig implements MillionaireConfig {
    /**
     * 1-out-of-n (with n = 2^l) ot protocol config.
     */
    private final LnotConfig lnotConfig;
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;

    private Rrk20MillionaireConfig(Rrk20MillionaireConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lnotConfig, builder.z2cConfig);
        lnotConfig = builder.lnotConfig;
        z2cConfig = builder.z2cConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public MillionaireFactory.MillionaireType getPtoType() {
        return MillionaireFactory.MillionaireType.RRK20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20MillionaireConfig> {
        /**
         * 1-out-of-n (with n = 2^l) ot protocol config.
         */
        private final LnotConfig lnotConfig;
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
            if (silent) {
                lnotConfig = LnotFactory.createCacheConfig(securityModel);
            } else {
                lnotConfig = LnotFactory.createDirectConfig(securityModel);
            }
        }

        @Override
        public Rrk20MillionaireConfig build() {
            return new Rrk20MillionaireConfig(this);
        }
    }
}
