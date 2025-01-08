package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * @author Feng Han
 * @date 2024/8/6
 */
public class PstReceiverThread extends Thread {
    /**
     * receiver
     */
    private final PstReceiver receiver;
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
    /**
     * whether the corresponding permutation is from the left part of net
     */
    private final boolean isLeft;

    PstReceiverThread(PstReceiver receiver, int batchNum, int eachNum, int byteLength, boolean isLeft) {
        this(receiver, batchNum, eachNum, byteLength, null, isLeft);
    }

    PstReceiverThread(PstReceiver receiver, int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput, boolean isLeft) {
        this.receiver = receiver;
        this.batchNum = batchNum;
        this.eachNum = eachNum;
        this.byteLength = byteLength;
        this.preSenderOutput = preSenderOutput;
        this.isLeft = isLeft;
    }

    BstReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.shareTranslate(batchNum, eachNum, byteLength, preSenderOutput, isLeft);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
