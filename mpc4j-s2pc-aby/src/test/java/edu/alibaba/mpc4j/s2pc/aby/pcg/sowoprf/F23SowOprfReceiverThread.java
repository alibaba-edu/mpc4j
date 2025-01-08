package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * (F2, F3)-sowOPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/10/24
 */
class F23SowOprfReceiverThread extends Thread {
    /**
     * receiver
     */
    private final F23SowOprfReceiver receiver;
    /**
     * inputs
     */
    private final byte[][] inputs;
    /**
     * pre-computed COT receiver output
     */
    private final CotReceiverOutput preCotReceiverOutput;
    /**
     * receiver output
     */
    private byte[][] receiverOutput;

    F23SowOprfReceiverThread(F23SowOprfReceiver receiver, byte[][] inputs, CotReceiverOutput preCotReceiverOutput) {
        this.receiver = receiver;
        this.inputs = inputs;
        this.preCotReceiverOutput = preCotReceiverOutput;
    }

    byte[][] getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(inputs.length);
            receiverOutput = receiver.oprf(inputs, preCotReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
