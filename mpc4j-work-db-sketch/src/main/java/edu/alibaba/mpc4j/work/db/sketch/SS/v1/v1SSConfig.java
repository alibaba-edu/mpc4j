package edu.alibaba.mpc4j.work.db.sketch.SS.v1;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.hzf22.Hzf22AggConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory;

/**
 * v1 MG config
 */
public class v1SSConfig extends AbstractMultiPartyPtoConfig implements SSConfig {
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
     * config of order select
     */
    private final OrderSelectConfig orderSelectConfig;
    /**
     * config of agg
     */
    private final AggConfig aggConfig;
    /**
     * config of pop
     */
    private final PopConfig popConfig;

    private v1SSConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        groupSumConfig = builder.groupSumConfig;
        pgSortConfig = builder.pgSortConfig;
        orderSelectConfig = builder.orderSelectConfig;
        aggConfig = builder.aggConfig;
        popConfig = builder.popConfig;
    }

    @Override
    public SSFactory.MGPtoType getPtoType() {
        return SSFactory.MGPtoType.V1;
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

    public OrderSelectConfig getOrderSelectConfig() {return orderSelectConfig;
    }

    public AggConfig getAggConfig() {
        return aggConfig;
    }

    public PopConfig getPopConfig() {
        return popConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<v1SSConfig> {
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
         * config of order select
         */
        private final OrderSelectConfig orderSelectConfig;
        /**
         * config of agg
         */
        private AggConfig aggConfig;
        /**
         * config of pop
         */
        private final PopConfig popConfig;
        /**
         * type of comparator
         */
        private ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            groupSumConfig = GroupSumFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pgSortConfig = PgSortFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            orderSelectConfig = OrderSelectFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            comparatorType = ComparatorType.TREE_COMPARATOR;
            aggConfig = new Hzf22AggConfig.Builder(malicious).setComparatorType(comparatorType).build();
            popConfig = PopFactory.createDefaultConfig(malicious);
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.pgSortConfig = pgSortConfig;
            return this;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            aggConfig = new Hzf22AggConfig.Builder(malicious).setComparatorType(comparatorType).build();
            return this;
        }

        @Override
        public v1SSConfig build() {
            return new v1SSConfig(this);
        }
    }
}
