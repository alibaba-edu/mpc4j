package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * pre-compute 1-out-of-n (with n = 2^l) receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
class PreCotReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PreCotReceiver receiver;
    /**
     * pre-compute receiver output
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * the choices
     */
    private final boolean[] choices;
    /**
     * the output
     */
    private CotReceiverOutput receiverOutput;

    PreCotReceiverThread(PreCotReceiver receiver, CotReceiverOutput preReceiverOutput, boolean[] choices) {
        this.receiver = receiver;
        this.preReceiverOutput = preReceiverOutput;
        this.choices = choices;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.receive(preReceiverOutput, choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
