package edu.alibaba.mpc4j.common.circuit.z2.comparator;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * Comparator Factory.
 *
 * @author Feng Han
 * @date 2025/2/27
 */
public class ComparatorFactory {
    /**
     * Private constructor.
     */
    private ComparatorFactory() {
        // empty
    }

    /**
     * Adder type enums.
     */
    public enum ComparatorType {
        /**
         * tree-form parallel execution
         */
        TREE_COMPARATOR,
        /**
         * serial-form
         */
        SERIAL_COMPARATOR,
    }

    /**
     * get the required and gate number of comparator
     */
    public static int getAndGateNum(ComparatorType type, int l) {
        return switch (type) {
            case TREE_COMPARATOR -> (int) Math.ceil(2.5 * l) - LongUtils.ceilLog2(l) + 1;
            case SERIAL_COMPARATOR -> l;
            default ->
                throw new IllegalArgumentException("Invalid " + ComparatorType.class.getSimpleName() + ": " + type.name());
        };
    }

    /**
     * Creates a adder.
     *
     * @param type    type of adder.
     * @param circuit z2 integer circuit.
     * @return a adder.
     */
    public static Comparator createComparator(ComparatorType type, Z2IntegerCircuit circuit) {
        return switch (type) {
            case TREE_COMPARATOR -> new TreeComparator(circuit);
            case SERIAL_COMPARATOR -> new SerialComparator(circuit);
            default ->
                throw new IllegalArgumentException("Invalid " + ComparatorType.class.getSimpleName() + ": " + type.name());
        };
    }
}
