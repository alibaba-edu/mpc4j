package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * COT receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class CotReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final CotReceiver receiver;
    /**
     * choices
     */
    private final boolean[] choices;
    /**
     * update num
     */
    private final int updateNum;
    /**
     * the receiver output
     */
    private CotReceiverOutput receiverOutput;

    CotReceiverThread(CotReceiver receiver, boolean[] choices) {
        this(receiver, choices, choices.length);
    }

    CotReceiverThread(CotReceiver receiver, boolean[] choices, int updateNum) {
        this.receiver = receiver;
        this.choices = choices;
        this.updateNum = updateNum;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(updateNum);
            receiverOutput = receiver.receive(choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
