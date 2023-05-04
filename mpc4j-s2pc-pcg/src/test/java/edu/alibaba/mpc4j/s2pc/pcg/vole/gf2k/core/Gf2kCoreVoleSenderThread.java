package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * GF2K-core VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
class Gf2kCoreVoleSenderThread extends Thread {
    /**
     * the sender
     */
    private final Gf2kCoreVoleSender sender;
    /**
     * x
     */
    private final byte[][] x;
    /**
     * the sender output
     */
    private Gf2kVoleSenderOutput senderOutput;

    Gf2kCoreVoleSenderThread(Gf2kCoreVoleSender sender, byte[][] x) {
        this.sender = sender;
        this.x = x;
    }

    Gf2kVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(x.length);
            senderOutput = sender.send(x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
