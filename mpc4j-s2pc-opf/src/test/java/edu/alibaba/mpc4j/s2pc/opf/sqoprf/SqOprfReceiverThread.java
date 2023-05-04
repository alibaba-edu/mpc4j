package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

/**
 * single-query OPRF receiver
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final SqOprfReceiver receiver;
    /**
     * the inputs
     */
    private final byte[][] inputs;
    /**
     * the receiver output
     */
    private SqOprfReceiverOutput receiverOutput;

    SqOprfReceiverThread(SqOprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    SqOprfReceiverOutput getReceiverOutput() {
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
