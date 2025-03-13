package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;

/**
 * sorting party config, using radix sorting
 *
 * @author Feng Han
 * @date 2024/02/27
 */
public class RadixPgSortConfig extends AbstractMultiPartyPtoConfig implements PgSortConfig {
    /**
     * configure of permuation
     */
    private final PermuteConfig permuteConfig;
    private RadixPgSortConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        permuteConfig = builder.permuteConfig;
    }

    @Override
    public PgSortType getSortType() {
        return PgSortType.RADIX_PG_SORT;
    }

    @Override
    public boolean isStable() {
        return true;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {

    }

    public PermuteConfig getPermuteConfig() {
        return permuteConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RadixPgSortConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of permutation
         */
        private final PermuteConfig permuteConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            permuteConfig = PermuteFactory.createDefaultConfig(securityModel);
        }

        @Override
        public RadixPgSortConfig build() {
            return new RadixPgSortConfig(this);
        }
    }
}
