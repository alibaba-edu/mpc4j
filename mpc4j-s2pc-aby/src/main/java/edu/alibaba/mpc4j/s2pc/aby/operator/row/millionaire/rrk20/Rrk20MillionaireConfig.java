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
    /**
     * bit length of split block
     */
    private final int m;

    private Rrk20MillionaireConfig(Rrk20MillionaireConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lnotConfig, builder.z2cConfig);
        lnotConfig = builder.lnotConfig;
        z2cConfig = builder.z2cConfig;
        m = builder.m;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public int getM() {
        return m;
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
        /**
         * bit length of split block
         */
        private int m;

        public Builder(SecurityModel securityModel, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
            lnotConfig = LnotFactory.createDefaultConfig(securityModel, silent);
            m = 4;
        }

        @Override
        public Rrk20MillionaireConfig build() {
            return new Rrk20MillionaireConfig(this);
        }

        public Builder setM(int m) {
            this.m = m;
            return this;
        }
    }
}
