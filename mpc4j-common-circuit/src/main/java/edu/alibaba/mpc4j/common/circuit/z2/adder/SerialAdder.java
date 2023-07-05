package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Serial adder using linear prefix network, which essentially gives a Ripple Carry Adder in pre-computation mode.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class SerialAdder extends AbstractParallelPrefixAdder {

    public SerialAdder(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    protected void addPrefix() throws MpcAbortException {
        Tuple input = tuples[l - 1];
        for (int i = l - 2; i >= 0; i--) {
            tuples[i] = op(tuples[i], input);
            input = tuples[i];
        }
    }
}
