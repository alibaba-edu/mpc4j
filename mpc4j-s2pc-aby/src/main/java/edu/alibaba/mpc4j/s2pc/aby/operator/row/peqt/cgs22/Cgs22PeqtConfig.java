package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * CGS22 private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PeqtConfig extends AbstractMultiPartyPtoConfig implements PeqtConfig {
    /**
     * Z2 circuit config
     */
    private final Z2cConfig z2cConfig;
    /**
     * LNOT config
     */
    private final LnotConfig lnotConfig;
    /**
     * bit length of split block
     */
    private int m;

    private Cgs22PeqtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.lnotConfig);
        z2cConfig = builder.z2cConfig;
        lnotConfig = builder.lnotConfig;
        m = builder.m;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public int getM() {
        return m;
    }

    @Override
    public PeqtFactory.PeqtType getPtoType() {
        return PeqtFactory.PeqtType.CGS22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22PeqtConfig> {
        /**
         * Boolean circuit config
         */
        private final Z2cConfig z2cConfig;
        /**
         * LNOT config
         */
        private final LnotConfig lnotConfig;
        /**
         * bit length of split block
         */
        private int m;

        public Builder(SecurityModel securityModel, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
            lnotConfig = LnotFactory.createDefaultConfig(securityModel, silent);
            m = 4;
        }

        public Builder setM(int m) {
            this.m = m;
            return this;
        }

        @Override
        public Cgs22PeqtConfig build() {
            return new Cgs22PeqtConfig(this);
        }
    }
}
