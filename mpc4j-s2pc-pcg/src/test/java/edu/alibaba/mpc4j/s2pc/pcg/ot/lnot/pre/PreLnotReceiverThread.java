package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

/**
 * pre-compute 1-out-of-n (with n = 2^l) OT receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class PreLnotReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PreLnotReceiver receiver;
    /**
     * pre-compute receiver output
     */
    private final LnotReceiverOutput preReceiverOutput;
    /**
     * the choice array
     */
    private final int[] choiceArray;
    /**
     * the output
     */
    private LnotReceiverOutput receiverOutput;

    PreLnotReceiverThread(PreLnotReceiver receiver, LnotReceiverOutput preReceiverOutput, int[] choiceArray) {
        this.receiver = receiver;
        this.preReceiverOutput = preReceiverOutput;
        this.choiceArray = choiceArray;
    }

    LnotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.receive(preReceiverOutput, choiceArray);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
