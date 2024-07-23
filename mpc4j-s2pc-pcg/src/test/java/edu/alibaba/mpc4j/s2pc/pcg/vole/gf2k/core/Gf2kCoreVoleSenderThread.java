package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * GF2K-core-VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
class Gf2kCoreVoleSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kCoreVoleSender sender;
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * x
     */
    private final byte[][] x;
    /**
     * the sender output
     */
    private Gf2kVoleSenderOutput senderOutput;

    Gf2kCoreVoleSenderThread(Gf2kCoreVoleSender sender, Sgf2k field, byte[][] x) {
        this.sender = sender;
        this.field = field;
        this.x = x;
    }

    Gf2kVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(field.getSubfieldL());
            senderOutput = sender.send(x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
