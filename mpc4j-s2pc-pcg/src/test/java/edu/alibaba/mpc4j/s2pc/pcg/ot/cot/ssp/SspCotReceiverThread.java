package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Single single-point COT receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
class SspCotReceiverThread extends Thread {
    /**
     * receiver
     */
    private final SspCotReceiver receiver;
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
    private SspCotReceiverOutput receiverOutput;

    SspCotReceiverThread(SspCotReceiver receiver, int alpha, int num) {
        this(receiver, alpha, num, null);
    }

    SspCotReceiverThread(SspCotReceiver receiver, int alpha, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alpha = alpha;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    SspCotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(alpha, num) : receiver.receive(alpha, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
