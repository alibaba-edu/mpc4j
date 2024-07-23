package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * Batched single-point GF2K-VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
class Gf2kBspVoleSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kBspVoleSender sender;
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * α
     */
    private final int[] alphaArray;
    /**
     * eac num
     */
    private final int eachNum;
    /**
     * pre-computed sender output
     */
    private final Gf2kVoleSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private Gf2kBspVoleSenderOutput senderOutput;

    Gf2kBspVoleSenderThread(Gf2kBspVoleSender sender, Sgf2k field, int[] alphaArray, int eachNum) {
        this(sender, field, alphaArray, eachNum, null);
    }

    Gf2kBspVoleSenderThread(Gf2kBspVoleSender sender, Sgf2k field, int[] alphaArray, int eachNum,
                            Gf2kVoleSenderOutput preSenderOutput) {
        this.sender = sender;
        this.field = field;
        this.alphaArray = alphaArray;
        this.eachNum = eachNum;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kBspVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(field.getSubfieldL());
            senderOutput = sender.send(alphaArray, eachNum, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
