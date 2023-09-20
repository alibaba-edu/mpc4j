package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * MSP-COT receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class MspCotReceiverThread extends Thread {
    /**
     * receiver
     */
    private final MspCotReceiver receiver;
    /**
     * sparse num
     */
    private final int t;
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
    private MspCotReceiverOutput receiverOutput;

    MspCotReceiverThread(MspCotReceiver receiver, int t, int num) {
        this(receiver, t, num, null);
    }

    MspCotReceiverThread(MspCotReceiver receiver, int t, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.t = t;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    MspCotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(t, num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(t, num) : receiver.receive(t, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
