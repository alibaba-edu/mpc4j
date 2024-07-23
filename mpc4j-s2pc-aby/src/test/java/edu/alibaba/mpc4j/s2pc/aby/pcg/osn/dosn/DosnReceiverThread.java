package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Decision OSN receiver thread.
 *
 * @author Weiran Liu
 * @date 2021/09/20
 */
class DosnReceiverThread extends Thread {
    /**
     * receiver
     */
    private final DosnReceiver receiver;
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
    private DosnPartyOutput receiverOutput;

    DosnReceiverThread(DosnReceiver receiver, int[] pi, int byteLength) {
        this.receiver = receiver;
        this.byteLength = byteLength;
        this.pi = pi;
    }

    DosnPartyOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.dosn(pi, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
