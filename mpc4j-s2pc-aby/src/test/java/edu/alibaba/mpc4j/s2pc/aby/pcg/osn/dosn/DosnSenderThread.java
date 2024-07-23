package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Decision OSN sender thread.
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
class DosnSenderThread extends Thread {
    /**
     * sender
     */
    private final DosnSender sender;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * input vector
     */
    private final byte[][] inputVector;
    /**
     * sender output
     */
    private DosnPartyOutput senderOutput;

    DosnSenderThread(DosnSender sender, byte[][] inputVector, int byteLength) {
        this.sender = sender;
        this.byteLength = byteLength;
        this.inputVector = inputVector;
    }

    DosnPartyOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.dosn(inputVector, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
