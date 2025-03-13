package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory.AggPtoType;

/**
 * HZF22 aggregate function config.
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class Hzf22AggConfig extends AbstractMultiPartyPtoConfig implements AggConfig {
    /**
     * type of adder
     */
    private final ComparatorType comparatorType;

    private Hzf22AggConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        comparatorType = builder.comparatorType;
    }

    @Override
    public AggPtoType getPtoType() {
        return AggPtoType.HZF22;
    }

    @Override
    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22AggConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * type of adder
         */
        private ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            comparatorType = ComparatorType.SERIAL_COMPARATOR;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public Hzf22AggConfig build() {
            return new Hzf22AggConfig(this);
        }
    }
}
