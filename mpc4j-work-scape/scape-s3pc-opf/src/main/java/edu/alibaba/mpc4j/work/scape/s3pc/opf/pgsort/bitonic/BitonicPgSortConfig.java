package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;

/**
 * sorting party config, using bitonic sorting
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class BitonicPgSortConfig extends AbstractMultiPartyPtoConfig implements PgSortConfig {
    /**
     * whether the sorting process is stable
     */
    private final boolean isStable;
    /**
     * configure of permuation
     */
    private final PermuteConfig permuteConfig;
    /**
     * type of adder
     */
    private ComparatorType comparatorType;

    private BitonicPgSortConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        isStable = builder.isStable;
        permuteConfig = builder.permuteConfig;
        comparatorType = builder.comparatorType;
    }

    @Override
    public PgSortType getSortType() {
        return PgSortType.BITONIC_PG_SORT;
    }

    @Override
    public boolean isStable() {
        return isStable;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitonicPgSortConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * whether the sorting process is stable
         */
        private boolean isStable;
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
            isStable = false;
            permuteConfig = PermuteFactory.createDefaultConfig(malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
            comparatorType = ComparatorType.TREE_COMPARATOR;
        }

        public Builder setStableSign(boolean isStable){
            this.isStable = isStable;
            return this;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public BitonicPgSortConfig build() {
            return new BitonicPgSortConfig(this);
        }
    }
}
