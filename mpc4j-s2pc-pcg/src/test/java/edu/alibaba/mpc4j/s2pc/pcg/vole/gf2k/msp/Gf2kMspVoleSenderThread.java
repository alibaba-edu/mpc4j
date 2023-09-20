package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * GF2K-MSP-VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
class Gf2kMspVoleSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kMspVoleSender sender;
    /**
     * sparse num
     */
    private final int t;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed sender output
     */
    private final Gf2kVoleSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private Gf2kMspVoleSenderOutput senderOutput;

    Gf2kMspVoleSenderThread(Gf2kMspVoleSender sender, int t, int num) {
        this(sender, t, num, null);
    }

    Gf2kMspVoleSenderThread(Gf2kMspVoleSender sender, int t, int num, Gf2kVoleSenderOutput preSenderOutput) {
        this.sender = sender;
        this.t = t;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kMspVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(t, num);
            senderOutput = preSenderOutput == null ?
                sender.send(t, num) : sender.send(t, num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
