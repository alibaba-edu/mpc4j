package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.stream.IntStream;

/**
 * Parallel prefix adder using Kogge-Stone structure. The structure comes from the following paper:
 *
 * <p>
 * Kogge, Peter M., and Harold S. Stone. "A parallel algorithm for the efficient solution of a general class of
 * recurrence equations." IEEE transactions on computers 100.8 (1973): 786-793.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/6
 */
public class KoggeStoneAdder extends AbstractParallelPrefixAdder {

    public KoggeStoneAdder(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    protected void addPrefix() throws MpcAbortException {
        int gap = 1;
        while (gap < l) {
            int[] inputIndexes = IntStream.range(gap, l).toArray();
            int[] outputIndexes = IntStream.range(0, l - gap).toArray();
            updateCurrentLevel(inputIndexes, outputIndexes);
            gap <<= 1;
        }
    }
}
