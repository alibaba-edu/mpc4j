package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OPRF receiver thread
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class OprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final OprfReceiver receiver;
    /**
     * the inputs
     */
    private final byte[][] inputs;
    /**
     * the receiver output
     */
    private OprfReceiverOutput receiverOutput;

    OprfReceiverThread(OprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    OprfReceiverOutput getReceiverOutput() {
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