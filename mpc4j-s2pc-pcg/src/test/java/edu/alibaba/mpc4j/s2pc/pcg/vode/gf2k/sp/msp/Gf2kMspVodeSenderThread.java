package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-MSP-VODE sender thread.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gf2kMspVodeSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kMspVodeSender sender;
    /**
     * field
     */
    private final Dgf2k field;
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
    private final Gf2kVodeSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private Gf2kMspVodeSenderOutput senderOutput;

    Gf2kMspVodeSenderThread(Gf2kMspVodeSender sender, Dgf2k field, int t, int num) {
        this(sender, field, t, num, null);
    }

    Gf2kMspVodeSenderThread(Gf2kMspVodeSender sender, Dgf2k field, int t, int num,
                            Gf2kVodeSenderOutput preSenderOutput) {
        this.sender = sender;
        this.field = field;
        this.t = t;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kMspVodeSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(field.getSubfieldL());
            senderOutput = sender.send(t, num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
