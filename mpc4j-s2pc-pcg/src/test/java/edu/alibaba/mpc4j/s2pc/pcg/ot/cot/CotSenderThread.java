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
     * the sender output
     */
    private CotSenderOutput senderOutput;

    CotSenderThread(CotSender sender, byte[] delta, int num) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta, num, num);
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
