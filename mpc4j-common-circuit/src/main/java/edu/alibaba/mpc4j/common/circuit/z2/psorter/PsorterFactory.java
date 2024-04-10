package edu.alibaba.mpc4j.common.circuit.z2.psorter;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.bitonic.PermutableBitonicSorter;

/**
 * Factory of sorter for permutation generation.
 *
 * @author Feng Han
 * @date 2023/10/26
 */
public class PsorterFactory {
    /**
     * Private constructor.
     */
    private PsorterFactory() {
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
    }

    /**
     * Creates a sorter.
     *
     * @param type type of sorter.
     * @return a adder.
     */
    public static Psorter createPsorter(PsorterFactory.SorterTypes type, Z2IntegerCircuit circuit) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BITONIC:
                return new PermutableBitonicSorter(circuit);
            default:
                throw new IllegalArgumentException("Invalid " + PsorterFactory.SorterTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
