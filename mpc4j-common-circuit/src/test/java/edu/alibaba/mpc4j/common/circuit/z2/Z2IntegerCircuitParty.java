package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Mpc Z2 integer circuit party thread.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
class Z2IntegerCircuitParty {
    /**
     * the party
     */
    private final PlainZ2cParty party;
    /**
     * x0
     */
    private final PlainZ2Vector[][] x;
    /**
     * y0
     */
    private final PlainZ2Vector[][] y;
    /**
     * z0
     */
    private MpcZ2Vector[] z;
    /**
     * operator
     */
    private final Z2IntegerOperator operator;
    /**
     * config
     */
    private final Z2CircuitConfig config;

    Z2IntegerCircuitParty(PlainZ2cParty party, Z2IntegerOperator operator, PlainZ2Vector[] x, PlainZ2Vector[] y, Z2CircuitConfig config) {
        this(party, operator, new PlainZ2Vector[][]{x}, new PlainZ2Vector[][]{y}, config);
    }

    Z2IntegerCircuitParty(PlainZ2cParty party, Z2IntegerOperator operator, PlainZ2Vector[][] x, PlainZ2Vector[][] y, Z2CircuitConfig config) {
        this.party = party;
        this.operator = operator;
        this.x = x;
        this.y = y;
        this.config = config;
    }

    MpcZ2Vector[] getZ() {
        return z;
    }

    void run() {
        try {
            Z2IntegerCircuit circuit = new Z2IntegerCircuit(party, config);
            switch (operator) {
                case LEQ:
                    z = new PlainZ2Vector[]{(PlainZ2Vector) circuit.leq(x[0], y[0])};
                    break;
                case EQ:
                    z = new PlainZ2Vector[]{(PlainZ2Vector) circuit.eq(x[0], y[0])};
                    break;
                case ADD:
                    z = Arrays.stream(circuit.add(x[0], y[0])).toArray(MpcZ2Vector[]::new);
                    break;
                case MUL:
                    z = Arrays.stream(circuit.mul(x[0], y[0])).toArray(MpcZ2Vector[]::new);
                    break;
                case INCREASE_ONE:
                    z = Arrays.stream(circuit.increaseOne(x[0])).toArray(MpcZ2Vector[]::new);
                    break;
                case SUB:
                    z = Arrays.stream(circuit.sub(x[0], y[0])).toArray(MpcZ2Vector[]::new);
                    break;
                case SORT:
                    circuit.sort(x);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + operator.name() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
