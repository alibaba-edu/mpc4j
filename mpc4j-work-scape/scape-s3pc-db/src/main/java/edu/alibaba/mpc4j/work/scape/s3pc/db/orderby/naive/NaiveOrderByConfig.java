package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFactory.OrderByPtoType;

/**
 * naive order-by protocol config.
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class NaiveOrderByConfig extends AbstractMultiPartyPtoConfig implements OrderByConfig {
    /**
     * whether the sorting process is stable
     */
    private final boolean isStable;
    /**
     * type of adder
     */
    private ComparatorType comparatorType;

    private NaiveOrderByConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        isStable = builder.isStable;
        comparatorType = builder.comparatorType;
    }

    @Override
    public OrderByPtoType getOrderByPtoType() {
        return OrderByPtoType.ORDER_BY_NAIVE;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {
        this.comparatorType = comparatorType;
    }

    public boolean isStable() {
        return isStable;
    }

    public ComparatorType getComparatorTypes() {
        return comparatorType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveOrderByConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * whether the sorting process is stable
         */
        private boolean isStable;
        /**
         * type of adder
         */
        private ComparatorType comparatorType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            isStable = false;
            comparatorType = ComparatorType.TREE_COMPARATOR;
        }

        public Builder setStableSign(boolean isStable) {
            this.isStable = isStable;
            return this;
        }

        public Builder setComparatorType(ComparatorType comparatorType) {
            this.comparatorType = comparatorType;
            return this;
        }

        @Override
        public NaiveOrderByConfig build() {
            return new NaiveOrderByConfig(this);
        }
    }
}
