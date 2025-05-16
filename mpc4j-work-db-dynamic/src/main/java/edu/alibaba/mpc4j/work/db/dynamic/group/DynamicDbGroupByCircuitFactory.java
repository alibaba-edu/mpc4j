package edu.alibaba.mpc4j.work.db.dynamic.group;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * factory for dynamic db group by circuit
 *
 * @author Feng Han
 * @date 2025/3/7
 */
public class DynamicDbGroupByCircuitFactory implements PtoFactory {

    /**
     * private constructor.
     */
    private DynamicDbGroupByCircuitFactory() {
        // empty
    }

    /**
     * dynamic db group by circuit type
     */
    public enum DynamicDbGroupByCircuitType {
        /**
         * ZGC24 shortcut
         */
        ZGC24,
    }

    /**
     * create party.
     *
     * @param circuit z2 circuit
     */
    public static DynamicDbGroupByCircuit createCircuit(DynamicDbGroupByCircuitType type, Z2IntegerCircuit circuit) {
        return switch (type) {
            case ZGC24 -> new Zgc24DynamicDbGroupByCircuit(circuit);
        };
    }
}
