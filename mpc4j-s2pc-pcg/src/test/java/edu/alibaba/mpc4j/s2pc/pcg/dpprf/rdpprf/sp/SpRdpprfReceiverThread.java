package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * single-point RDPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class SpRdpprfReceiverThread extends Thread {
    /**
     * receiver
     */
    private final SpRdpprfReceiver receiver;
    /**
     * α
     */
    private final int alpha;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed receiver output
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private SpRdpprfReceiverOutput receiverOutput;

    SpRdpprfReceiverThread(SpRdpprfReceiver receiver, int alpha, int num) {
        this(receiver, alpha, num, null);
    }

    SpRdpprfReceiverThread(SpRdpprfReceiver receiver, int alpha, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alpha = alpha;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    SpRdpprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.puncture(alpha, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
