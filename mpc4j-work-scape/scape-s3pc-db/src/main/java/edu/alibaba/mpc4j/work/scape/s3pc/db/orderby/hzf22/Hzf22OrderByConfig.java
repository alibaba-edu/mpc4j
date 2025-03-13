package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFactory.OrderByPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22.Hzf22PgSortConfig;

/**
 * configure for HZF22 order-by protocol
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class Hzf22OrderByConfig extends AbstractMultiPartyPtoConfig implements OrderByConfig {
    /**
     * sorter config
     */
    private final PgSortConfig sortConfig;
    /**
     * permute config
     */
    private final PermuteConfig permuteConfig;

    private Hzf22OrderByConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        sortConfig = builder.sortConfig;
        permuteConfig = builder.permuteConfig;
    }

    @Override
    public OrderByPtoType getOrderByPtoType() {
        return OrderByPtoType.ORDER_BY_HZF22;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {
        sortConfig.setComparatorType(comparatorType);
    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public PgSortConfig getSortConfig() {
        return sortConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22OrderByConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * sorter config
         */
        private PgSortConfig sortConfig;
        /**
         * permute config
         */
        private final PermuteConfig permuteConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            sortConfig = new Hzf22PgSortConfig.Builder(malicious).build();
            permuteConfig = PermuteFactory.createDefaultConfig(malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        }

        public Builder setPgSortConfig(PgSortConfig pgSortConfig) {
            this.sortConfig = pgSortConfig;
            return this;
        }

        @Override
        public Hzf22OrderByConfig build() {
            return new Hzf22OrderByConfig(this);
        }
    }
}
