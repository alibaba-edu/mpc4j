package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;

/**
 * Pre-computed OSN receiver thread.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
class PosnReceiverThread extends Thread {
    /**
     * receiver
     */
    private final PosnReceiver receiver;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * permutation π
     */
    private final int[] pi;
    /**
     * pre-computed COT output
     */
    private final RosnReceiverOutput rosnReceiverOutput;
    /**
     * receiver output
     */
    private DosnPartyOutput receiverOutput;

    PosnReceiverThread(PosnReceiver receiver, int[] pi, int byteLength, RosnReceiverOutput rosnReceiverOutput) {
        this.receiver = receiver;
        this.byteLength = byteLength;
        this.pi = pi;
        this.rosnReceiverOutput = rosnReceiverOutput;
    }

    DosnPartyOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.posn(pi, byteLength, rosnReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
