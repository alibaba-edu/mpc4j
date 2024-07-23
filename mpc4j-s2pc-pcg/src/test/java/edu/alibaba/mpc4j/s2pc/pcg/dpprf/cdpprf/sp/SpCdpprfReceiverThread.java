package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * SP-CDPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
class SpCdpprfReceiverThread extends Thread {
    /**
     * receiver
     */
    private final SpCdpprfReceiver receiver;
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
    private SpCdpprfReceiverOutput receiverOutput;

    SpCdpprfReceiverThread(SpCdpprfReceiver receiver, int alpha, int num) {
        this(receiver, alpha, num, null);
    }

    SpCdpprfReceiverThread(SpCdpprfReceiver receiver, int alpha, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alpha = alpha;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    SpCdpprfReceiverOutput getReceiverOutput() {
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
