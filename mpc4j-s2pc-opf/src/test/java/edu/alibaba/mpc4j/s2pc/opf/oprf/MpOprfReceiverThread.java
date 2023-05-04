package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * multi-query OPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class MpOprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final MpOprfReceiver receiver;
    /**
     * the inputs
     */
    private final byte[][] inputs;
    /**
     * the receiver output
     */
    private MpOprfReceiverOutput receiverOutput;

    MpOprfReceiverThread(MpOprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    MpOprfReceiverOutput getReceiverOutput() {
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
