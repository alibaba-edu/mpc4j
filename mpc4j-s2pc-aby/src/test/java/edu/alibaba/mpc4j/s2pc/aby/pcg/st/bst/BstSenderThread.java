package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Batched Share Translation sender thread.
 *
 * @author Weiran Liu
 * @date 2024/4/24
 */
class BstSenderThread extends Thread {
    /**
     * sender
     */
    private final BstSender sender;
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

    BstSenderThread(BstSender sender, int[][] piArray, int byteLength) {
        this(sender, piArray, byteLength, null);
    }

    BstSenderThread(BstSender sender, int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput) {
        this.sender = sender;
        this.piArray = piArray;
        this.byteLength = byteLength;
        this.preReceiverOutput = preReceiverOutput;
    }

    BstSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.shareTranslate(piArray, byteLength, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
