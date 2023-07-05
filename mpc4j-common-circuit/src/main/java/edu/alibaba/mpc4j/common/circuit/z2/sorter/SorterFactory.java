package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.MultiplierFactory;

/**
 * Sorter Factory.
 *
 * @author Li Peng
 * @date 2023/6/12
 */
public class SorterFactory {
    /**
     * Private constructor.
     */
    private SorterFactory() {
        // empty
    }

    /**
     * Sorter type enums.
     */
    public enum SorterTypes {
        /**
         * Bitonic sorter.
         */
        BITONIC,
        /**
         * Randomized shell sorter.
         */
        RANDOMIZED_SHELL_SORTER
    }

    /**
     * Creates a sorter.
     *
     * @param type type of sorter.
     * @return a adder.
     */
    public static Sorter createSorter(SorterFactory.SorterTypes type, Z2IntegerCircuit circuit) {
        switch (type) {
            case BITONIC:
                return new BitonicSorter(circuit);
            case RANDOMIZED_SHELL_SORTER:
                return new RandomizedShellSorter(circuit);
            default:
                throw new IllegalArgumentException("Invalid " + MultiplierFactory.MultiplierTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
