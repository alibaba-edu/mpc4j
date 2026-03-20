package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectFactory.OrderSelectType;

/**
 * Configuration for quick sort based order select implementation.
 * Uses quick sorting algorithm for efficient oblivious order selection.
 */
public class QuickOrderSelectConfig extends AbstractMultiPartyPtoConfig implements OrderSelectConfig {
    /**
     * Configuration for quick sorting operations
     */
    private final QuickPgSortConfig quickPgSortConfig;

    private QuickOrderSelectConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        quickPgSortConfig = builder.quickPgSortConfig;
    }

    @Override
    public OrderSelectType getOrderSelectType() {
        return OrderSelectType.QUICK_ORDER_SELECT;
    }

    @Override
    public boolean isStable() {
        return true;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {
        quickPgSortConfig.setComparatorType(comparatorType);
    }

    /**
     * Get the quick sort configuration
     *
     * @return the quick sort configuration
     */
    public QuickPgSortConfig getQuickPgSortConfig() {
        return quickPgSortConfig;
    }

    /**
     * Builder for creating QuickOrderSelectConfig instances
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<QuickOrderSelectConfig> {
        /**
         * Whether to use malicious security model
         */
        private final boolean malicious;
        /**
         * Configuration for quick sorting operations
         */
        private final QuickPgSortConfig quickPgSortConfig;

        /**
         * Constructor for Builder
         *
         * @param malicious whether to use malicious security model
         */
        public Builder(boolean malicious) {
            this.malicious = malicious;
            quickPgSortConfig = (QuickPgSortConfig) PgSortFactory.createSortConfig(PgSortType.QUICK_PG_SORT, malicious);
        }

        @Override
        public QuickOrderSelectConfig build() {
            return new QuickOrderSelectConfig(this);
        }
    }
}
