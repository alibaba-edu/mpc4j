package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * Batched single-point GF2K-VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
class Gf2kBspVoleSenderThread extends Thread {
    /**
     * the sender
     */
    private final Gf2kBspVoleSender sender;
    /**
     * α
     */
    private final int[] alphaArray;
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
    private Gf2kBspVoleSenderOutput senderOutput;

    Gf2kBspVoleSenderThread(Gf2kBspVoleSender sender, int[] alphaArray, int num) {
        this(sender, alphaArray, num, null);
    }

    Gf2kBspVoleSenderThread(Gf2kBspVoleSender sender, int[] alphaArray, int num, Gf2kVoleSenderOutput preSenderOutput) {
        this.sender = sender;
        this.alphaArray = alphaArray;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kBspVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(alphaArray.length, num);
            senderOutput = preSenderOutput == null ?
                sender.send(alphaArray, num) : sender.send(alphaArray, num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
