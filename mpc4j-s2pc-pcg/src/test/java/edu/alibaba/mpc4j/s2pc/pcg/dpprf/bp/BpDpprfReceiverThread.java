package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * batch-point DPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class BpDpprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final BpDpprfReceiver receiver;
    /**
     * α array
     */
    private final int[] alphaArray;
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
    private BpDpprfReceiverOutput receiverOutput;

    BpDpprfReceiverThread(BpDpprfReceiver receiver, int[] alphaArray, int alphaBound) {
        this(receiver, alphaArray, alphaBound, null);
    }

    BpDpprfReceiverThread(BpDpprfReceiver receiver, int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.alphaBound = alphaBound;
        this.preReceiverOutput = preReceiverOutput;
    }

    BpDpprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(alphaArray.length, alphaBound);
            receiverOutput = preReceiverOutput == null ? receiver.puncture(alphaArray, alphaBound)
                : receiver.puncture(alphaArray, alphaBound, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
