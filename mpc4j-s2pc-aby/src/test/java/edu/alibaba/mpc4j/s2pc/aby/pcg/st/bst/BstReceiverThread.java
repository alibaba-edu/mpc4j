package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Bachted Share Translation receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/4/24
 */
class BstReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BstReceiver receiver;
    /**
     * batch num
     */
    private final int batchNum;
    /**
     * n
     */
    private final int eachNum;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * pre-computed COT sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * receiver output
     */
    private BstReceiverOutput receiverOutput;

    BstReceiverThread(BstReceiver receiver, int batchNum, int eachNum, int byteLength) {
        this(receiver, batchNum, eachNum, byteLength, null);
    }

    BstReceiverThread(BstReceiver receiver, int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput) {
        this.receiver = receiver;
        this.batchNum = batchNum;
        this.eachNum = eachNum;
        this.byteLength = byteLength;
        this.preSenderOutput = preSenderOutput;
    }

    BstReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.shareTranslate(batchNum, eachNum, byteLength, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
