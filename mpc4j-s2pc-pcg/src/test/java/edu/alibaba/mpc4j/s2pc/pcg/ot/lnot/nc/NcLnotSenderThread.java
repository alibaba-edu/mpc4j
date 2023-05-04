package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

/**
 * no-choice 1-out-of-n (with n = 2^l) sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class NcLnotSenderThread extends Thread {
    /**
     * sender
     */
    private final NcLnotSender sender;
    /**
     * l
     */
    private final int l;
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
    private final LnotSenderOutput senderOutput;

    NcLnotSenderThread(NcLnotSender sender, int l, int num, int round) {
        this.sender = sender;
        this.l = l;
        this.num = num;
        this.round = round;
        senderOutput = LnotSenderOutput.createEmpty(l);
    }

    LnotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(l, num);
            for (int index = 0; index < round; index++) {
                senderOutput.merge(sender.send());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}