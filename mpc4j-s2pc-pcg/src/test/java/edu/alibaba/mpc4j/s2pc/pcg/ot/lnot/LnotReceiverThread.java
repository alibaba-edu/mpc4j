package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 1-out-of-n (with n = 2^l) receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
class LnotReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final LnotReceiver receiver;
    /**
     * choice bit length
     */
    private final int l;
    /**
     * the choice array
     */
    private final int[] choiceArray;
    /**
     * update num
     */
    private final int updateNum;
    /**
     * the receiver output
     */
    private LnotReceiverOutput receiverOutput;

    LnotReceiverThread(LnotReceiver receiver, int l, int[] choiceArray) {
        this(receiver, l, choiceArray, choiceArray.length);
    }

    LnotReceiverThread(LnotReceiver receiver, int l, int[] choiceArray, int updateNum) {
        this.receiver = receiver;
        this.l = l;
        this.choiceArray = choiceArray;
        this.updateNum = updateNum;
    }

    LnotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, updateNum);
            receiverOutput = receiver.receive(choiceArray);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
