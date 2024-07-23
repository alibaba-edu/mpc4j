package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;

/**
 * Pre-computed OSN sender thread.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
class PosnSenderThread extends Thread {
    /**
     * sender
     */
    private final PosnSender sender;
    /**
     * input vector
     */
    private final byte[][] inputVector;
    /**
     * pre-computed COT output
     */
    private final RosnSenderOutput rosnSenderOutput;
    /**
     * sender output
     */
    private DosnPartyOutput senderOutput;

    PosnSenderThread(PosnSender sender, byte[][] inputVector, RosnSenderOutput rosnSenderOutput) {
        this.sender = sender;
        this.inputVector = inputVector;
        this.rosnSenderOutput = rosnSenderOutput;
    }

    DosnPartyOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.posn(inputVector, rosnSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
