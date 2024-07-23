package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * Single single-point GF2K-VODE sender thread.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gf2kSspVodeSenderThread extends Thread {
    /**
     * the sender
     */
    private final Gf2kSspVodeSender sender;
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * α
     */
    private final int alpha;
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
    private Gf2kSspVodeSenderOutput senderOutput;

    Gf2kSspVodeSenderThread(Gf2kSspVodeSender sender, Dgf2k field, int alpha, int num) {
        this(sender, field, alpha, num, null);
    }

    Gf2kSspVodeSenderThread(Gf2kSspVodeSender sender, Dgf2k field, int alpha, int num,
                            Gf2kVodeSenderOutput preSenderOutput) {
        this.sender = sender;
        this.field = field;
        this.alpha = alpha;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kSspVodeSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(field.getSubfieldL());
            senderOutput = sender.send(alpha, num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
