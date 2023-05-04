package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

/**
 * pre-compute 1-out-of-n (with n = 2^l) OT sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class PreLnotSenderThread extends Thread {
    /**
     * the sender
     */
    private final PreLnotSender sender;
    /**
     * pre-compute sender output
     */
    private final LnotSenderOutput preSenderOutput;
    /**
     * the output
     */
    private LnotSenderOutput senderOutput;

    PreLnotSenderThread(PreLnotSender sender, LnotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.preSenderOutput = preSenderOutput;
    }

    LnotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.send(preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
