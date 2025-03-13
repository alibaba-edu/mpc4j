package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;

/**
 * mixed sorting party config, choice sorting method based on input and somewhat-opt strategy
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class OptPgSortConfig extends AbstractMultiPartyPtoConfig implements PgSortConfig {
    /**
     * configure of radix sorter
     */
    private final RadixPgSortConfig radixPgSortConfig;
    /**
     * configure of quick sorter
     */
    private final QuickPgSortConfig quickPgSortConfig;

    private OptPgSortConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        radixPgSortConfig = builder.radixPgSortConfig;
        quickPgSortConfig = builder.quickPgSortConfig;
    }

    @Override
    public PgSortType getSortType() {
        return PgSortType.OPT_PG_SORT;
    }

    @Override
    public boolean isStable() {
        return true;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {
        quickPgSortConfig.setComparatorType(comparatorType);
    }

    public RadixPgSortConfig getRadixPgSortConfig() {
        return radixPgSortConfig;
    }

    public QuickPgSortConfig getQuickPgSortConfig() {
        return quickPgSortConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OptPgSortConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of radix sorter
         */
        private final RadixPgSortConfig radixPgSortConfig;
        /**
         * configure of quick sorter
         */
        private QuickPgSortConfig quickPgSortConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            radixPgSortConfig = new RadixPgSortConfig.Builder(malicious).build();
            quickPgSortConfig = new QuickPgSortConfig.Builder(malicious).build();
        }

        public Builder setQuickPgSortConfig(QuickPgSortConfig quickPgSortConfig) {
            this.quickPgSortConfig = quickPgSortConfig;
            return this;
        }

        @Override
        public OptPgSortConfig build() {
            return new OptPgSortConfig(this);
        }
    }
}

