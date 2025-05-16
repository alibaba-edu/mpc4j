package edu.alibaba.mpc4j.work.db.dynamic.join.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * dynamic db pk-pk join circuit factory.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class DynamicDbPkPkJoinCircuitFactory implements PtoFactory {

    /**
     * private constructor.
     */
    private DynamicDbPkPkJoinCircuitFactory() {
        // empty
    }

    /**
     * dynamic db pk-pk join circuit type.
     */
    public enum DynamicDbPkPkJoinCircuitType {
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
    public static DynamicDbPkPkJoinCircuit createCircuit(DynamicDbPkPkJoinCircuitType type, Z2IntegerCircuit circuit) {
        return switch (type) {
            case ZGC24 -> new Zgc24DynamicDbPkPkJoinCircuit(circuit);
        };
    }
}
