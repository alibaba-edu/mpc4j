package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Batched single-point COT sender thread.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class BspCotSenderThread extends Thread {
    /**
     * sender
     */
    private final BspCotSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * batch num
     */
    private final int batchNum;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed COT sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private BspCotSenderOutput senderOutput;

    BspCotSenderThread(BspCotSender sender, byte[] delta, int batchNum, int num) {
        this(sender, delta, batchNum, num, null);
    }

    BspCotSenderThread(BspCotSender sender, byte[] delta, int batchNum, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.batchNum = batchNum;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    BspCotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta, batchNum, num);
            senderOutput = preSenderOutput == null ? sender.send(batchNum, num) : sender.send(batchNum, num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
