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
     * the receiver output
     */
    private CotReceiverOutput receiverOutput;

    CotReceiverThread(CotReceiver receiver, boolean[] choices) {
        this.receiver = receiver;
        this.choices = choices;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(choices.length, choices.length);
            receiverOutput = receiver.receive(choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
