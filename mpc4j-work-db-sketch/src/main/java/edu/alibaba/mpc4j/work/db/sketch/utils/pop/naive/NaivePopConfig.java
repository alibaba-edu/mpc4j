package edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory.PopPtoType;

/**
 * naive pop config.
 */
public class NaivePopConfig extends AbstractMultiPartyPtoConfig implements PopConfig {
    /**
     * z2 circuit config
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

    public Z2CircuitConfig getCircuitConfig() {
        return circuitConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaivePopConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * z2 circuit config
         */
        private final Z2CircuitConfig circuitConfig;

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
