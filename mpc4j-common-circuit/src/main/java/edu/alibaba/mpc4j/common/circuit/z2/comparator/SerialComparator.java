package edu.alibaba.mpc4j.common.circuit.z2.comparator;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * compare two values in serial way, inspired by Ripple Carry Adder
 *
 * @author Feng Han
 * @date 2025/2/27
 */
public class SerialComparator extends AbstractZ2Circuit implements Comparator {

    public SerialComparator(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    public MpcZ2Vector leq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        MpcZ2Vector leqSign = party.and(xiArray[xiArray.length - 1], party.xor(xiArray[xiArray.length - 1], yiArray[yiArray.length - 1]));
        party.noti(leqSign);
        // next.leqSign = (x \oplus y) \cdot (y \oplus leqSign) \oplus leqSign
        for (int i = xiArray.length - 2; i >= 0; i--) {
            MpcZ2Vector notEq = party.xor(xiArray[i], yiArray[i]);
            MpcZ2Vector yXorC = party.xor(yiArray[i], leqSign);
            party.xori(leqSign, party.and(notEq, yXorC));
        }
        return leqSign;
    }
}
