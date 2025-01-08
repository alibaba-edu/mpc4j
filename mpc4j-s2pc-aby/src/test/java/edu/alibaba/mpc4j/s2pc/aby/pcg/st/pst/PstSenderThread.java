package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * @author Feng Han
 * @date 2024/8/6
 */
public class PstSenderThread extends Thread {
    /**
     * sender
     */
    private final PstSender sender;
    /**
     * permutation π array
     */
    private final int[][] piArray;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * pre-computed COT receiver output
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * sender output
     */
    private BstSenderOutput senderOutput;
    /**
     * whether the corresponding permutation is from the left part of net
     */
    private final boolean isLeft;

    PstSenderThread(PstSender sender, int[][] piArray, int byteLength, boolean isLeft) {
        this(sender, piArray, byteLength, null, isLeft);
    }

    PstSenderThread(PstSender sender, int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput, boolean isLeft) {
        this.sender = sender;
        this.piArray = piArray;
        this.byteLength = byteLength;
        this.preReceiverOutput = preReceiverOutput;
        this.isLeft = isLeft;
    }

    BstSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.shareTranslate(piArray, byteLength, preReceiverOutput, isLeft);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
