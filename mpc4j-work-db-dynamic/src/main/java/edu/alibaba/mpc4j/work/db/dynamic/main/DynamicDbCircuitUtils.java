package edu.alibaba.mpc4j.work.db.dynamic.main;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.work.db.dynamic.DynamicDbCircuitConfig;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuitFactory.DynamicDbOrderByCircuitType;

import java.util.Properties;

/**
 * create circuit configure
 *
 * @author Feng Han
 * @date 2025/3/24
 */
public class DynamicDbCircuitUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";
    /**
     * orderByCircuit type key.
     */
    private static final String ORDER_BY_TYPE = "order_by_type";

    /**
     * private constructor.
     */
    private DynamicDbCircuitUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static DynamicDbCircuitConfig createConfig(Properties properties) {
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        Z2CircuitConfig circuitConfig = new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build();
        DynamicDbOrderByCircuitType orderByCircuitType = MainPtoConfigUtils.readEnum(DynamicDbOrderByCircuitType.class, properties, ORDER_BY_TYPE);
        return new DynamicDbCircuitConfig.Builder().setZ2CircuitConfig(circuitConfig).setOrderByCircuitType(orderByCircuitType).build();
    }
}
