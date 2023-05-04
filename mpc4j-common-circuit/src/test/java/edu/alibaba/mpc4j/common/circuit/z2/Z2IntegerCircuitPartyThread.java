package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Mpc Z2 integer circuit party thread.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
class Z2IntegerCircuitPartyThread extends Thread {
    /**
     * the party
     */
    private final PlainBcParty party;
    /**
     * x0
     */
    private final PlainZ2Vector[] x;
    /**
     * y0
     */
    private final PlainZ2Vector[] y;
    /**
     * z0
     */
    private MpcZ2Vector[] z;
    /**
     * operator
     */
    private final IntegerOperator operator;

    Z2IntegerCircuitPartyThread(PlainBcParty party, IntegerOperator operator, PlainZ2Vector[] x, PlainZ2Vector[] y) {
        this.party = party;
        this.operator = operator;
        this.x = x;
        this.y = y;
    }

    MpcZ2Vector[] getZ() {
        return z;
    }

    @Override
    public void run() {
        try {
            Z2IntegerCircuit circuit = new Z2IntegerCircuit(party);
            switch (operator) {
                case LEQ:
                    z = new PlainZ2Vector[]{(PlainZ2Vector) circuit.leq(x, y)};
                    break;
                case EQ:
                    z = new PlainZ2Vector[]{(PlainZ2Vector) circuit.eq(x, y)};
                    break;
                case ADD:
                    z = Arrays.stream(circuit.add(x, y)).toArray(MpcZ2Vector[]::new);
                    break;
                case INCREASE_ONE:
                    z = Arrays.stream(circuit.increaseOne(x)).toArray(MpcZ2Vector[]::new);
                    break;
                case SUB:
                    z = Arrays.stream(circuit.sub(x, y)).toArray(MpcZ2Vector[]::new);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + operator.name() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
