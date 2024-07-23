package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Single Share Translation receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
class SstReceiverThread extends Thread {
    /**
     * receiver
     */
    private final SstReceiver receiver;
    /**
     * n
     */
    private final int num;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * pre-computed COT sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * receiver output
     */
    private SstReceiverOutput receiverOutput;

    SstReceiverThread(SstReceiver receiver, int num, int byteLength) {
        this(receiver, num, byteLength, null);
    }

    SstReceiverThread(SstReceiver receiver, int num, int byteLength, CotSenderOutput preSenderOutput) {
        this.receiver = receiver;
        this.num = num;
        this.byteLength = byteLength;
        this.preSenderOutput = preSenderOutput;
    }

    SstReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.shareTranslate(num, byteLength, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
