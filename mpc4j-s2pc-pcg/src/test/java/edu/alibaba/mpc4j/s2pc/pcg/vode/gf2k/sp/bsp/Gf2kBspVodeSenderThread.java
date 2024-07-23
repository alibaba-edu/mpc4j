package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-BSP-VODE sender thread.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gf2kBspVodeSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kBspVodeSender sender;
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * α
     */
    private final int[] alphaArray;
    /**
     * each num
     */
    private final int eachNum;
    /**
     * pre-computed sender output
     */
    private final Gf2kVodeSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private Gf2kBspVodeSenderOutput senderOutput;

    Gf2kBspVodeSenderThread(Gf2kBspVodeSender sender, Dgf2k field, int[] alphaArray, int eachNum) {
        this(sender, field, alphaArray, eachNum, null);
    }

    Gf2kBspVodeSenderThread(Gf2kBspVodeSender sender, Dgf2k field, int[] alphaArray, int eachNum,
                            Gf2kVodeSenderOutput preSenderOutput) {
        this.sender = sender;
        this.field = field;
        this.alphaArray = alphaArray;
        this.eachNum = eachNum;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kBspVodeSenderOutput getSenderOutput() {
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
