package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * Single single-point GF2K-VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class Gf2kSspVoleSenderThread extends Thread {
    /**
     * the sender
     */
    private final Gf2kSspVoleSender sender;
    /**
     * field
     */
    private final Sgf2k field;
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
    private final Gf2kVoleSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private Gf2kSspVoleSenderOutput senderOutput;

    Gf2kSspVoleSenderThread(Gf2kSspVoleSender sender, Sgf2k field, int alpha, int num) {
        this(sender, field, alpha, num, null);
    }

    Gf2kSspVoleSenderThread(Gf2kSspVoleSender sender, Sgf2k field, int alpha, int num,
                            Gf2kVoleSenderOutput preSenderOutput) {
        this.sender = sender;
        this.field = field;
        this.alpha = alpha;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kSspVoleSenderOutput getSenderOutput() {
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
