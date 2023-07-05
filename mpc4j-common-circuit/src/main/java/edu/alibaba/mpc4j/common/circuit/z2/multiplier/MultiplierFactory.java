package edu.alibaba.mpc4j.common.circuit.z2.multiplier;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;

/**
 * Multiplier Factory.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class MultiplierFactory {
    /**
     * Private constructor.
     */
    private MultiplierFactory() {
        // empty
    }

    /**
     * Multiplier type enums.
     */
    public enum MultiplierTypes {
        /**
         * Shift/add multiplier.
         */
        SHIFT_ADD,
    }

    /**
     * Creates a multiplier.
     *
     * @param type    type of adder.
     * @param circuit z2 integer circuit.
     * @return a adder.
     */
    public static Multiplier createMultiplier(MultiplierTypes type, Z2IntegerCircuit circuit) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case SHIFT_ADD:
                return new ShiftAddMultiplier(circuit);
            default:
                throw new IllegalArgumentException("Invalid " + MultiplierTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
