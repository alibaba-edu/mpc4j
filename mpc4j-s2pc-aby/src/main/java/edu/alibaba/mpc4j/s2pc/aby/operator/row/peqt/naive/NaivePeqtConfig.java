package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;

/**
 * naive private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePeqtConfig extends AbstractMultiPartyPtoConfig implements PeqtConfig {
    /**
     * Z2 circuit config
     */
    private final Z2cConfig z2cConfig;

    private NaivePeqtConfig(Builder builder) {
        super(builder.z2cConfig);
        z2cConfig = builder.z2cConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public PeqtFactory.PeqtType getPtoType() {
        return PeqtFactory.PeqtType.NAIVE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaivePeqtConfig> {
        /**
         * Boolean circuit config
         */
        private Z2cConfig z2cConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public NaivePeqtConfig build() {
            return new NaivePeqtConfig(this);
        }
    }
}
