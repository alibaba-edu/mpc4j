package edu.alibaba.mpc4j.common.circuit.prefix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Parallel prefix tree of Brent-Kung structure. The structure comes from the following paper:
 *
 * <p>
 * Brent, and Kung. "A regular layout for parallel adders." IEEE transactions on Computers 100.3 (1982): 260-264.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class BrentKungTree extends AbstractPrefixTree {

    public BrentKungTree(PrefixOp prefixOp) {
        super(prefixOp);
    }

    @Override
    public void addPrefix(int l) throws MpcAbortException {
        int ceilL = 1 << (BigInteger.valueOf(l - 1).bitLength());
        // offset denotes the distance of index in a perfect binary tree (with ceilL leaves) and index in the ture tree (with l nodes).
        int offset = ceilL - l;
        int logL = LongUtils.ceilLog2(ceilL);
        int blockNum = ceilL / 2;
        int blockSize = 2;
        // reduction will be performed in a perfect binary tree (with ceilL leaves) instead of the ture tree,
        // while we should avoid iterations in the nodes which beyond ture indexes by determining if index >= 0.
        // first tree-reduction
        for (int i = 0; i < logL; i++) {
            int invalidNodesNumFirst = obtainInvalidNodesNumFirst(offset, blockSize);
            int[] inputIndexes = new int[blockNum - invalidNodesNumFirst];
            int[] outputIndexes = new int[blockNum - invalidNodesNumFirst];
            for (int j = 0; j < blockNum; j++) {
                int inputIndex = ceilL - (j * blockSize + blockSize / 2) - offset;
                if (inputIndex >= 0) {
                    int currentIndex = ceilL - ((j + 1) * blockSize) - offset;
                    if (currentIndex >= 0) {
                        inputIndexes[j] = inputIndex;
                        outputIndexes[j] = currentIndex;
                    }
                }
            }
            if (inputIndexes.length != 0) {
                prefixOp.updateCurrentLevel(inputIndexes, outputIndexes);
            }
            blockNum >>= 1;
            blockSize <<= 1;
        }
        // second tree-reduction
        blockNum = 2;
        blockSize = ceilL / 2;
        for (int i = 0; i < logL - 1; i++) {
            int invalidNodesNumSecond = obtainInvalidNodesNumSecond(offset, blockSize);
            int[] inputIndexes = new int[blockNum - 1 - invalidNodesNumSecond];
            int[] outputIndexes = new int[blockNum - 1 - invalidNodesNumSecond];
            for (int j = 0; j < blockNum - 1; j++) {
                int inputIndex = ceilL - (j + 1) * blockSize - offset;
                if (inputIndex >= 0) {
                    int currentIndex = ceilL - (j + 1) * blockSize - blockSize / 2 - offset;
                    if (currentIndex >= 0) {
                        inputIndexes[j] = inputIndex;
                        outputIndexes[j] = currentIndex;
                    }
                }
            }
            if (inputIndexes.length != 0) {
                prefixOp.updateCurrentLevel(inputIndexes, outputIndexes);
            }
            blockNum <<= 1;
            blockSize >>= 1;
        }
    }

    /**
     * Obtain number of invalid nodes in the perfect tree in first tree iteration.
     *
     * @param offset    offset
     * @param blockSize block size.
     * @return number of invalid nodes
     */
    private int obtainInvalidNodesNumFirst(int offset, int blockSize) {
        int n = offset / blockSize;
        if (offset > n * blockSize) {
            return n + 1;
        } else {
            return n;
        }
    }

    /**
     * Obtain number of invalid nodes in the perfect tree in second tree iteration.
     *
     * @param offset    offset
     * @param blockSize block size.
     * @return number of invalid nodes
     */
    private int obtainInvalidNodesNumSecond(int offset, int blockSize) {
        int n = offset / blockSize;
        if (offset - n * blockSize - 1 >= blockSize / 2) {
            return n + 1;
        } else {
            return n;
        }
    }
}
