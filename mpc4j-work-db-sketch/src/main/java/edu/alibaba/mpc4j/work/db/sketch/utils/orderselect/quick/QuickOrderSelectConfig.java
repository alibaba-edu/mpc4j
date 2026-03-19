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
 * order select party config, using quick sorting
 */
public class QuickOrderSelectConfig extends AbstractMultiPartyPtoConfig implements OrderSelectConfig {
    /**
     * configure of quick sorting
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

    public QuickPgSortConfig getQuickPgSortConfig() {
        return quickPgSortConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<QuickOrderSelectConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of quick sorting
         */
        private final QuickPgSortConfig quickPgSortConfig;

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
