package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Single Share Translation sender thread.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
class SstSenderThread extends Thread {
    /**
     * sender
     */
    private final SstSender sender;
    /**
     * permutation π
     */
    private final int[] pi;
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
    private SstSenderOutput senderOutput;

    SstSenderThread(SstSender sender, int[] pi, int byteLength) {
        this(sender, pi, byteLength, null);
    }

    SstSenderThread(SstSender sender, int[] pi, int byteLength, CotReceiverOutput preReceiverOutput) {
        this.sender = sender;
        this.pi = pi;
        this.byteLength = byteLength;
        this.preReceiverOutput = preReceiverOutput;
    }

    SstSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.shareTranslate(pi, byteLength, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
