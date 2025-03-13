package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory.GroupSumPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;

/**
 * Configure of HZF22 group sum protocol
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class Hzf22GroupSumConfig extends AbstractMultiPartyPtoConfig implements GroupSumConfig {
    /**
     * type of adder
     */
    private final ComparatorType comparatorType;
    /**
     * config of oblivious traversal
     */
    private final TraversalConfig traversalConfig;

    private Hzf22GroupSumConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        comparatorType = builder.comparatorType;
        traversalConfig = builder.traversalConfig;
    }

    @Override
    public GroupSumPtoType getPtoType() {
        return GroupSumPtoType.HZF22;
    }

    @Override
    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public TraversalConfig getTraversalConfig() {
        return traversalConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22GroupSumConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * type of adder
         */
        private ComparatorType comparatorType;
        /**
         * config of oblivious traversal
         */
        private final TraversalConfig traversalConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            comparatorType = ComparatorType.TREE_COMPARATOR;
            traversalConfig = TraversalFactory.createDefaultConfig(malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public Hzf22GroupSumConfig build() {
            return new Hzf22GroupSumConfig(this);
        }
    }
}
