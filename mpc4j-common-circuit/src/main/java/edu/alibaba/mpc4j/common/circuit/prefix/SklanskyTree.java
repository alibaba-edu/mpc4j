package edu.alibaba.mpc4j.common.circuit.prefix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Parallel prefix tree of Sklansky structure. The structure comes from the following paper:
 *
 * <p>
 * Sklansky, Jack. "Conditional-sum addition logic." IRE Transactions on Electronic computers 2 (1960): 226-231.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class SklanskyTree extends AbstractPrefixTree {

    public SklanskyTree(PrefixOp prefixOp) {
        super(prefixOp);
    }

    @Override
    public void addPrefix(int l) throws MpcAbortException {
        int ceilL = 1 << (BigInteger.valueOf(l - 1).bitLength());
        // offset denotes the distance of index in a perfect binary tree (with ceilL leaves) and index in the ture tree (with l nodes).
        int offset = ceilL - l;
        int logL = LongUtils.ceilLog2(ceilL);
        // reduction will be performed in a perfect binary tree (with ceilL leaves) instead of the ture tree,
        // while we should avoid iterations in the nodes which beyond ture indexes by determining if index >= 0.
        int blockNum = ceilL / 2;
        int blockSize = 2;
        for (int i = 0; i < logL; i++) {
            int invalidNodesNum = obtainInvalidNodesNum(offset, blockSize);
            int[] inputIndexes = new int[ceilL / 2 - invalidNodesNum];
            int[] outputIndexes = new int[ceilL / 2 - invalidNodesNum];
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
            prefixOp.updateCurrentLevel(inputIndexes, outputIndexes);
            blockNum >>= 1;
            blockSize <<= 1;
        }
    }

    /**
     * Obtain number of invalid nodes in the perfect tree.
     *
     * @param offset    offset
     * @param blockSize block size.
     * @return number of invalid nodes
     */
    private int obtainInvalidNodesNum(int offset, int blockSize) {
        if (offset >= blockSize) {
            int n = offset / blockSize;
            if (offset >= n * blockSize + blockSize / 2) {
                return n * blockSize / 2 + blockSize / 2;
            } else {
                return n * blockSize / 2 + offset - n * blockSize;
            }
        } else {
            return Math.min(offset, blockSize / 2);
        }
    }
}
