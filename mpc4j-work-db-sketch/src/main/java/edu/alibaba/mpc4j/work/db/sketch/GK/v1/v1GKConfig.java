package edu.alibaba.mpc4j.work.db.sketch.GK.v1;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKConfig;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKFactory;

/**
 * v1 GK config
 */
public class v1GKConfig extends AbstractMultiPartyPtoConfig implements GKConfig {
    /**
     * config of oblivious permutation
     */
    private final PermuteConfig permuteConfig;
    /**
     * config of group sum
     */
    private final GroupSumConfig groupSumConfig;
    /**
     * config of pg sorter
     */
    private final PgSortConfig pgSortConfig;
    /**
     * config of travesal
     */
    private final TraversalConfig traversalConfig;

    private v1GKConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        groupSumConfig = builder.groupSumConfig;
        pgSortConfig = builder.pgSortConfig;
        traversalConfig = builder.traversalConfig;
    }

    @Override
    public GKFactory.GKPtoType getPtoType() {
        return GKFactory.GKPtoType.V1;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public GroupSumConfig getGroupSumConfig() {
        return groupSumConfig;
    }

    public PgSortConfig getPgSortConfig() {
        return pgSortConfig;
    }

    public TraversalConfig getTraversalConfig() {
        return traversalConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<v1GKConfig> {
        /**
         * malicious or not
         */
        private final boolean malicious;
        /**
         * config of oblivious permutation
         */
        private final PermuteConfig permuteConfig;
        /**
         * config of group sum
         */
        private final GroupSumConfig groupSumConfig;
        /**
         * config of pg sorter
         */
        private PgSortConfig pgSortConfig;
        /**
         * config of travesal
         */
        private final TraversalConfig traversalConfig;
        /**
         * type of comparator
         */
        private ComparatorFactory.ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupSumConfig = GroupSumFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            traversalConfig = TraversalFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            comparatorType = ComparatorFactory.ComparatorType.TREE_COMPARATOR;
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        public Builder setComparatorType(ComparatorFactory.ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public v1GKConfig build() {
            return new v1GKConfig(this);
        }
    }
}

