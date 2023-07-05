package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * COT sender thread.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class CotSenderThread extends Thread {
    /**
     * the sender
     */
    private final CotSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
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
    private CotSenderOutput senderOutput;

    CotSenderThread(CotSender sender, byte[] delta, int num) {
        this(sender, delta, num, num);
    }

    CotSenderThread(CotSender sender, byte[] delta, int num, int updateNum) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
        this.updateNum = updateNum;
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta, updateNum);
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
