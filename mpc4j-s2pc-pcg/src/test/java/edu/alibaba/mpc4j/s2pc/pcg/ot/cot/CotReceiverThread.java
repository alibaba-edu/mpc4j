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
     * expect num
     */
    private final int expectNum;
    /**
     * the receiver output
     */
    private CotReceiverOutput receiverOutput;

    CotReceiverThread(CotReceiver receiver, boolean[] choices) {
        this(receiver, choices, choices.length);
    }

    CotReceiverThread(CotReceiver receiver, boolean[] choices, int expectNum) {
        this.receiver = receiver;
        this.choices = choices;
        this.expectNum = expectNum;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(expectNum);
            receiverOutput = receiver.receive(choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
