package edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory.PopPtoType;

/**
 * Configuration for the naive implementation of Pop (Permute-and-Open) protocol.
 * Uses basic Z2 circuit operations for the pop functionality.
 */
public class NaivePopConfig extends AbstractMultiPartyPtoConfig implements PopConfig {
    /**
     * Z2 circuit configuration for boolean circuit operations
     */
    private final Z2CircuitConfig circuitConfig;

    private NaivePopConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        circuitConfig = builder.circuitConfig;
    }

    @Override
    public PopPtoType getPtoType() {
        return PopPtoType.NAIVE;
    }

    /**
     * Get the Z2 circuit configuration
     *
     * @return the Z2 circuit config
     */
    public Z2CircuitConfig getCircuitConfig() {
        return circuitConfig;
    }

    /**
     * Builder for creating NaivePopConfig instances
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaivePopConfig> {
        /**
         * Whether to use malicious security model
         */
        private final boolean malicious;
        /**
         * Z2 circuit configuration
         */
        private final Z2CircuitConfig circuitConfig;

        /**
         * Constructor for Builder
         *
         * @param malicious whether to use malicious security model
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
            circuitConfig = new Z2CircuitConfig.Builder().build();
        }

        @Override
        public NaivePopConfig build() {
            return new NaivePopConfig(this);
        }
    }
}
