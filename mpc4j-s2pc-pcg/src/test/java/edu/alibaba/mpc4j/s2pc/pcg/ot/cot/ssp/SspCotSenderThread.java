package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Single single-point COT sender thread.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
class SspCotSenderThread extends Thread {
    /**
     * sender
     */
    private final SspCotSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
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
    private SspCotSenderOutput senderOutput;

    SspCotSenderThread(SspCotSender sender, byte[] delta, int num) {
        this(sender, delta, num, null);
    }

    SspCotSenderThread(SspCotSender sender, byte[] delta, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    SspCotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta, num);
            senderOutput = preSenderOutput == null ? sender.send(num) : sender.send(num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
