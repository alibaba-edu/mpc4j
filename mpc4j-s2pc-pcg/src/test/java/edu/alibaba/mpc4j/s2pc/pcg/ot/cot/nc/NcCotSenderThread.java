package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * no-choice COT sender thread.
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class NcCotSenderThread extends Thread {
    /**
     * sender
     */
    private final NcCotSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * num
     */
    private final int num;
    /**
     * round
     */
    private final int round;
    /**
     * the sender output
     */
    private final CotSenderOutput senderOutput;

    NcCotSenderThread(NcCotSender sender, byte[] delta, int num, int round) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
        this.round = round;
        senderOutput = CotSenderOutput.createEmpty(delta);
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta, num);
            for (int index = 0; index < round; index++) {
                senderOutput.merge(sender.send());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}