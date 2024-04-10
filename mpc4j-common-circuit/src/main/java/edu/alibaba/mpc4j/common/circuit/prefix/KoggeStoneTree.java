package edu.alibaba.mpc4j.common.circuit.prefix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.stream.IntStream;

/**
 * Parallel prefix tree of Kogge-Stone structure. The structure comes from the following paper:
 *
 * <p>
 * Kogge, Peter M., and Harold S. Stone. "A parallel algorithm for the efficient solution of a general class of
 * recurrence equations." IEEE transactions on computers 100.8 (1973): 786-793.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/6
 */
public class KoggeStoneTree extends AbstractPrefixTree {

    public KoggeStoneTree(PrefixOp prefixOp) {
        super(prefixOp);
    }

    @Override
    public void addPrefix(int l) throws MpcAbortException {
        int gap = 1;
        while (gap < l) {
            int[] inputIndexes = IntStream.range(gap, l).toArray();
            int[] outputIndexes = IntStream.range(0, l - gap).toArray();
            prefixOp.updateCurrentLevel(inputIndexes, outputIndexes);
            gap <<= 1;
        }
    }
}
