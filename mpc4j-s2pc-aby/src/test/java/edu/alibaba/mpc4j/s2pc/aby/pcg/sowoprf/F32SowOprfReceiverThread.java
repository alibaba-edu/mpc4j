package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * (F3, F2)-sowOPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class F32SowOprfReceiverThread extends Thread {
    /**
     * receiver
     */
    private final F32SowOprfReceiver receiver;
    /**
     * inputs
     */
    private final byte[][] inputs;
    /**
     * receiver output
     */
    private byte[][] receiverOutput;

    F32SowOprfReceiverThread(F32SowOprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    byte[][] getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(inputs.length);
            receiverOutput = receiver.oprf(inputs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
