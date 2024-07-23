package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Random OSN receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class RosnReceiverThread extends Thread {
    /**
     * receiver
     */
    private final RosnReceiver receiver;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * permutation π
     */
    private final int[] pi;
    /**
     * receiver output
     */
    private RosnReceiverOutput receiverOutput;

    RosnReceiverThread(RosnReceiver receiver, int[] pi, int byteLength) {
        this.receiver = receiver;
        this.pi = pi;
        this.byteLength = byteLength;
    }

    RosnReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.rosn(pi, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
