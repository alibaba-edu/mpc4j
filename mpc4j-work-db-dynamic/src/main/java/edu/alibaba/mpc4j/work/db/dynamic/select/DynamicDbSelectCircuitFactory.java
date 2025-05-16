package edu.alibaba.mpc4j.work.db.dynamic.select;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * dynamic db select circuit factory.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class DynamicDbSelectCircuitFactory implements PtoFactory {

    /**
     * private constructor.
     */
    private DynamicDbSelectCircuitFactory() {
        // empty
    }

    /**
     * dynamic db select circuit type
     */
    public enum DynamicDbSelectCircuitType {
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
    public static DynamicDbSelectCircuit createCircuit(DynamicDbSelectCircuitType type, Z2IntegerCircuit circuit) {
        return switch (type) {
            case ZGC24 -> new Zgc24DynamicDbSelectCircuit(circuit);
        };
    }
}
