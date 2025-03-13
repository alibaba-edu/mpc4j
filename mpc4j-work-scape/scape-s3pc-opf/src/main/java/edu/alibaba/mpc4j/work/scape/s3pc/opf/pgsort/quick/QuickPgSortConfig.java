package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;

/**
 * sorting party config, using quick sorting
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class QuickPgSortConfig extends AbstractMultiPartyPtoConfig implements PgSortConfig {
    /**
     * configure of permuation
     */
    private final PermuteConfig permuteConfig;
    /**
     * type of adder
     */
    private ComparatorType comparatorType;

    private QuickPgSortConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
        comparatorType = builder.comparatorType;
    }

    @Override
    public PgSortType getSortType() {
        return PgSortType.QUICK_PG_SORT;
    }

    @Override
    public boolean isStable() {
        return true;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {
        this.comparatorType = comparatorType;
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<QuickPgSortConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of permuation
         */
        private final PermuteConfig permuteConfig;
        /**
         * type of adder
         */
        private ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            permuteConfig = PermuteFactory.createDefaultConfig(malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
            comparatorType = ComparatorType.TREE_COMPARATOR;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public QuickPgSortConfig build() {
            return new QuickPgSortConfig(this);
        }
    }
}

