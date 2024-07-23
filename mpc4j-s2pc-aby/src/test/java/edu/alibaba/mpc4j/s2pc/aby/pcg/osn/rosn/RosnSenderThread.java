package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Random OSN sender thread.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class RosnSenderThread extends Thread {
    /**
     * sender
     */
    private final RosnSender sender;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * num
     */
    private final int num;
    /**
     * sender output
     */
    private RosnSenderOutput senderOutput;

    RosnSenderThread(RosnSender sender, int num, int byteLength) {
        this.sender = sender;
        this.num = num;
        this.byteLength = byteLength;
    }

    RosnSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.rosn(num, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
