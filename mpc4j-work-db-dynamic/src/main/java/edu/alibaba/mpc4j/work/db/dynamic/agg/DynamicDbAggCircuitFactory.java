package edu.alibaba.mpc4j.work.db.dynamic.agg;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * @author Feng Han
 * @date 2025/3/10
 */
public class DynamicDbAggCircuitFactory implements PtoFactory {

    /**
     * private constructor.
     */
    private DynamicDbAggCircuitFactory() {
        // empty
    }

    /**
     * dynamic db agg circuit type
     */
    public enum DynamicDbAggCircuitType {
        /**
         * ZGC24 shortcut
         */
        ZGC24,
    }

    public static DynamicDbAggCircuit createCircuit(DynamicDbAggCircuitType type, Z2IntegerCircuit circuit) {
        return switch (type) {
            case ZGC24 -> new Zgc24DynamicDbAggCircuit(circuit);
        };
    }
}
