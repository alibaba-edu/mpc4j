package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Permuted Matrix Private Equality Test receiver thread.
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
public class PmPeqtReceiverThread extends Thread {
    /**
     * receiver
     */
    private final PmPeqtReceiver receiver;
    /**
     * byte length
     */
    private final int byteLength;
    /**
     * receiver input
     */
    private final byte[][][] inputMatrix;
    /**
     * row
     */
    private final int row;
    /**
     * column
     */
    private final int column;
    /**
     * output
     */
    private boolean[][] receiverOutput;

    PmPeqtReceiverThread(PmPeqtReceiver receiver, byte[][][] inputMatrix, int byteLength, int row, int column) {
        this.receiver = receiver;
        this.byteLength = byteLength;
        this.inputMatrix = inputMatrix;
        this.row = row;
        this.column = column;
    }

    boolean[][] getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(row, column);
            receiverOutput = receiver.pmPeqt(inputMatrix, byteLength, row, column);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
