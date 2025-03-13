package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory.GroupSumPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;

/**
 * the config for HZF22-extension group sum protocol
 *
 * @author Feng Han
 * @date 2025/3/3
 */
public class Hzf22ExtGroupSumConfig extends AbstractMultiPartyPtoConfig implements GroupSumConfig {
    /**
     * type of adder
     */
    private final ComparatorType comparatorType;
    /**
     * config of oblivious traversal
     */
    private final PermuteConfig permuteConfig;

    private Hzf22ExtGroupSumConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        comparatorType = builder.comparatorType;
        permuteConfig = builder.permuteConfig;
    }

    @Override
    public GroupSumPtoType getPtoType() {
        return GroupSumPtoType.HZF22EXT;
    }

    @Override
    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22ExtGroupSumConfig> {
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
        private final PermuteConfig permuteConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            comparatorType = ComparatorType.TREE_COMPARATOR;
            permuteConfig = PermuteFactory.createDefaultConfig(malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public Hzf22ExtGroupSumConfig build() {
            return new Hzf22ExtGroupSumConfig(this);
        }
    }
}
