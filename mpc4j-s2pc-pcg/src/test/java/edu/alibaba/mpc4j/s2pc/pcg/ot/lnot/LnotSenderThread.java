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
     * update num
     */
    private final int updateNum;
    /**
     * the sender output
     */
    private LnotSenderOutput senderOutput;

    LnotSenderThread(LnotSender sender, int l, int num) {
        this(sender, l, num, num);
    }

    LnotSenderThread(LnotSender sender, int l, int num, int updateNum) {
        this.sender = sender;
        this.l = l;
        this.num = num;
        this.updateNum = updateNum;
    }

    LnotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(l, updateNum);
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
