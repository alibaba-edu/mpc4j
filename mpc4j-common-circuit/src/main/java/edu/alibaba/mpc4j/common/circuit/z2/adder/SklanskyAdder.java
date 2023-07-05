package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Parallel prefix adder using Sklansky structure. The structure comes from the following paper:
 *
 * <p>
 * Sklansky, Jack. "Conditional-sum addition logic." IRE Transactions on Electronic computers 2 (1960): 226-231.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class SklanskyAdder extends AbstractParallelPrefixAdder {

    public SklanskyAdder(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    protected void addPrefix() throws MpcAbortException {
        int ceilL = 1 << (BigInteger.valueOf(tuples.length - 1).bitLength());
        // offset denotes the distance of index in a perfect binary tree (with ceilL leaves) and index in the ture tree (with l nodes).
        int offset = ceilL - l;
        int logL = LongUtils.ceilLog2(ceilL);
        // reduction will be performed in a perfect binary tree (with ceilL leaves) instead of the ture tree,
        // while we should avoid iterations in the nodes which beyond ture indexes by determining if index >= 0.
        int blockNum = ceilL / 2;
        int blockSize = 2;
        for (int i = 0; i < logL; i++) {
            int[] inputIndexes = new int[blockNum * (blockSize / 2)];
            int[] outputIndexes = new int[blockNum * (blockSize / 2)];
            for (int j = 0; j < blockNum; j++) {
                int inputIndex = ceilL - (j * blockSize + blockSize / 2) - offset;
                if (inputIndex >= 0) {
                    for (int k = 0; k < blockSize / 2; k++) {
                        int currentIndex = ceilL - (j * blockSize + blockSize / 2 + k) - 1 - offset;
                        if (currentIndex >= 0) {
                            inputIndexes[j * blockSize / 2 + k] = inputIndex;
                            outputIndexes[j * blockSize / 2 + k] = currentIndex;
                        }
                    }
                }
            }
            updateCurrentLevel(inputIndexes, outputIndexes);
            blockNum >>= 1;
            blockSize <<= 1;
        }
    }
}
