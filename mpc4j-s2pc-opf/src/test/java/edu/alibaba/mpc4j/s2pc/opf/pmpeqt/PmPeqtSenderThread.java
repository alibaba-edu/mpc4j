package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Permuted Matrix Private Equality Test sender thread.
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
public class PmPeqtSenderThread extends Thread {
    /**
     * sender
     */
    private final PmPeqtSender sender;
    /**
     * byte length
     */
    private final int byteLength;
    /**
     * sender input
     */
    private final byte[][][] inputMatrix;
    /**
     * row permutation map
     */
    private final int[] rowPermutationMap;
    /**
     * column permutation map
     */
    private final int[] columnPermutationMap;

    PmPeqtSenderThread(PmPeqtSender sender, byte[][][] inputMatrix, int byteLength, int[] rowPermutationMap,
                       int[] columnPermutationMap) {
        this.sender = sender;
        this.byteLength = byteLength;
        this.inputMatrix = inputMatrix;
        this.rowPermutationMap = rowPermutationMap;
        this.columnPermutationMap = columnPermutationMap;
    }

    @Override
    public void run() {
        try {
            sender.init(rowPermutationMap.length, columnPermutationMap.length);
            sender.pmPeqt(inputMatrix, rowPermutationMap, columnPermutationMap, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
