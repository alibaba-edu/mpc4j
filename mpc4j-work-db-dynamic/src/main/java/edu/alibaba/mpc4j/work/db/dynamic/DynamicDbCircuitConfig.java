package edu.alibaba.mpc4j.work.db.dynamic;

import edu.alibaba.mpc4j.common.circuit.CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.work.db.dynamic.agg.DynamicDbAggCircuitFactory.DynamicDbAggCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.group.DynamicDbGroupByCircuitFactory.DynamicDbGroupByCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.DynamicDbPkPkJoinCircuitFactory.DynamicDbPkPkJoinCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuitFactory.DynamicDbOrderByCircuitType;
import edu.alibaba.mpc4j.work.db.dynamic.select.DynamicDbSelectCircuitFactory.DynamicDbSelectCircuitType;

/**
 * interface for dynamic database protocol configuration
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class DynamicDbCircuitConfig implements CircuitConfig {
    /**
     * z2 circuit config
     */
    private final Z2CircuitConfig circuitConfig;
    /**
     * order-by circuit
     */
    private final DynamicDbOrderByCircuitType orderByCircuitType;
    /**
     * group-by circuit
     */
    private final DynamicDbGroupByCircuitType groupByCircuitType;
    /**
     * join circuit
     */
    private final DynamicDbPkPkJoinCircuitType joinCircuitType;
    /**
     * select circuit
     */
    private final DynamicDbSelectCircuitType selectCircuitType;
    /**
     * aggregate circuit
     */
    private final DynamicDbAggCircuitType aggCircuitType;


    public DynamicDbCircuitConfig(Builder builder) {
        circuitConfig = builder.circuitConfig;
        orderByCircuitType = builder.orderByCircuitType;
        groupByCircuitType = builder.groupByCircuitType;
        joinCircuitType = builder.joinCircuitType;
        selectCircuitType = builder.selectCircuitType;
        aggCircuitType = builder.aggCircuitType;
    }

    public Z2CircuitConfig getCircuitConfig() {
        return circuitConfig;
    }

    public DynamicDbOrderByCircuitType getOrderByCircuitType() {
        return orderByCircuitType;
    }

    public DynamicDbGroupByCircuitType getGroupByCircuitType() {
        return groupByCircuitType;
    }

    public DynamicDbPkPkJoinCircuitType getJoinCircuitType() {
        return joinCircuitType;
    }

    public DynamicDbSelectCircuitType getSelectCircuitType() {
        return selectCircuitType;
    }

    public DynamicDbAggCircuitType getAggCircuitType() {
        return aggCircuitType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DynamicDbCircuitConfig> {
        /**
         * z2 circuit config
         */
        private Z2CircuitConfig circuitConfig;
        /**
         * order-by circuit
         */
        private DynamicDbOrderByCircuitType orderByCircuitType;
        /**
         * group-by circuit
         */
        private final DynamicDbGroupByCircuitType groupByCircuitType;
        /**
         * join circuit
         */
        private final DynamicDbPkPkJoinCircuitType joinCircuitType;
        /**
         * aggregate circuit
         */
        private final DynamicDbSelectCircuitType selectCircuitType;
        /**
         * aggregate circuit
         */
        private final DynamicDbAggCircuitType aggCircuitType;

        public Builder() {
            circuitConfig = new Z2CircuitConfig.Builder().setComparatorType(ComparatorType.TREE_COMPARATOR).build();
            orderByCircuitType = DynamicDbOrderByCircuitType.ZGC24;
            groupByCircuitType = DynamicDbGroupByCircuitType.ZGC24;
            joinCircuitType = DynamicDbPkPkJoinCircuitType.ZGC24;
            selectCircuitType = DynamicDbSelectCircuitType.ZGC24;
            aggCircuitType = DynamicDbAggCircuitType.ZGC24;
        }

        public Builder setZ2CircuitConfig(Z2CircuitConfig circuitConfig) {
            this.circuitConfig = circuitConfig;
            return this;
        }

        public Builder setOrderByCircuitType(DynamicDbOrderByCircuitType orderByCircuitType) {
            this.orderByCircuitType = orderByCircuitType;
            return this;
        }

        @Override
        public DynamicDbCircuitConfig build() {
            return new DynamicDbCircuitConfig(this);
        }
    }
}
