package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * single-point DPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class SpDpprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final SpDpprfReceiver receiver;
    /**
     * α
     */
    private final int alpha;
    /**
     * α bound
     */
    private final int alphaBound;
    /**
     * pre-computed receiver output
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private SpDpprfReceiverOutput receiverOutput;

    SpDpprfReceiverThread(SpDpprfReceiver receiver, int alpha, int alphaBound) {
        this(receiver, alpha, alphaBound, null);
    }

    SpDpprfReceiverThread(SpDpprfReceiver receiver, int alpha, int alphaBound, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alpha = alpha;
        this.alphaBound = alphaBound;
        this.preReceiverOutput = preReceiverOutput;
    }

    SpDpprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(alphaBound);
            receiverOutput = preReceiverOutput == null ?
                receiver.puncture(alpha, alphaBound) : receiver.puncture(alpha, alphaBound, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
