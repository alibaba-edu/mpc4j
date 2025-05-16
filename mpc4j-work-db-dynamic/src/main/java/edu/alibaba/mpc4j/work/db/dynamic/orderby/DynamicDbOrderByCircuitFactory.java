package edu.alibaba.mpc4j.work.db.dynamic.orderby;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * factory for dynamic db order by circuit
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class DynamicDbOrderByCircuitFactory implements PtoFactory {

    /**
     * private constructor.
     */
    private DynamicDbOrderByCircuitFactory() {
        // empty
    }

    /**
     * dynamic db order by circuit type
     */
    public enum DynamicDbOrderByCircuitType {
        /**
         * ZGC24 shortcut
         */
        ZGC24,
        /**
         * Our optimization for ZGC24 shortcut
         */
        ZGC24_OPT,
    }

    /**
     * create party.
     *
     * @param circuit z2 circuit
     */
    public static DynamicDbOrderByCircuit createCircuit(DynamicDbOrderByCircuitType type, Z2IntegerCircuit circuit) {
        return switch (type) {
            case ZGC24 -> new Zgc24DynamicDbOrderByCircuit(circuit);
            case ZGC24_OPT -> new Zgc24OptDynamicDbOrderByCircuit(circuit);
        };
    }
}
