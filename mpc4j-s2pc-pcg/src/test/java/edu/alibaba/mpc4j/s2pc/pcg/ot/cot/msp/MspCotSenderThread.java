package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * MSP-COT sender thread.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class MspCotSenderThread extends Thread {
    /**
     * sender
     */
    private final MspCotSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * sparse num
     */
    private final int t;
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
    private MspCotSenderOutput senderOutput;

    MspCotSenderThread(MspCotSender sender, byte[] delta, int t, int num) {
        this(sender, delta, t, num, null);
    }

    MspCotSenderThread(MspCotSender sender, byte[] delta, int t, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.t = t;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    MspCotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta, t, num);
            senderOutput = preSenderOutput == null ? sender.send(t, num) : sender.send(t, num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
