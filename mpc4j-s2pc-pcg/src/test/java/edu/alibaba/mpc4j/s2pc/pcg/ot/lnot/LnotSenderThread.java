package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 1-out-of-n (with n = 2^l) sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
class LnotSenderThread extends Thread {
    /**
     * the sender
     */
    private final LnotSender sender;
    /**
     * l
     */
    private final int l;
    /**
     * num
     */
    private final int num;
    /**
     * the sender output
     */
    private LnotSenderOutput senderOutput;

    LnotSenderThread(LnotSender sender, int l, int num) {
        this.sender = sender;
        this.l = l;
        this.num = num;
    }

    LnotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(l, num, num);
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
