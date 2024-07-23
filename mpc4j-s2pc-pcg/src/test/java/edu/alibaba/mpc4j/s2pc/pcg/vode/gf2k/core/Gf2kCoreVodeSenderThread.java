package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-core-VODE sender thread.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
class Gf2kCoreVodeSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kCoreVodeSender sender;
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * x
     */
    private final byte[][] x;
    /**
     * the sender output
     */
    private Gf2kVodeSenderOutput senderOutput;

    Gf2kCoreVodeSenderThread(Gf2kCoreVodeSender sender, Dgf2k field, byte[][] x) {
        this.sender = sender;
        this.field = field;
        this.x = x;
    }

    Gf2kVodeSenderOutput getSenderOutput() {
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
