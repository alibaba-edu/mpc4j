package edu.alibaba.mpc4j.s2pc.opf.osorter.bitonic;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.opf.osorter.ObSortConfig;
import edu.alibaba.mpc4j.s2pc.opf.osorter.ObSortFactory.ObSortType;

/**
 * bitonic sorter configure
 *
 * @author Feng Han
 * @date 2024/10/8
 */
public class BitonicSorterConfig extends AbstractMultiPartyPtoConfig implements ObSortConfig {
    /**
     * z2c circuit config
     */
    private final Z2CircuitConfig z2CircuitConfig;
    /**
     * z2c party config
     */
    private final Z2cConfig z2cConfig;

    public BitonicSorterConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig);
        z2CircuitConfig = builder.z2CircuitConfig;
        z2cConfig = builder.z2cConfig;
    }

    @Override
    public ObSortType getPtoType() {
        return ObSortType.BITONIC;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public Z2CircuitConfig getZ2CircuitConfig() {
        return z2CircuitConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitonicSorterConfig> {
        /**
         * z2c circuit config
         */
        private Z2CircuitConfig z2CircuitConfig;
        /**
         * z2c party config
         */
        private Z2cConfig z2cConfig;

        public Builder(boolean silent) {
            z2CircuitConfig = new Z2CircuitConfig.Builder().build();
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setZ2CircuitConfig(Z2CircuitConfig z2CircuitConfig) {
            this.z2CircuitConfig = z2CircuitConfig;
            return this;
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public BitonicSorterConfig build() {
            return new BitonicSorterConfig(this);
        }
    }
}
