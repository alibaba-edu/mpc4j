package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;

/**
 * ZP64-core VOLE sender thread.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
class Zp64CoreVoleSenderThread extends Thread {
    /**
     * the sender
     */
    private final Zp64CoreVoleSender sender;
    /**
     * the Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * x
     */
    private final long[] x;
    /**
     * the sender output
     */
    private Zp64VoleSenderOutput senderOutput;

    Zp64CoreVoleSenderThread(Zp64CoreVoleSender sender, Zp64 zp64, long[] x) {
        this.sender = sender;
        this.zp64 = zp64;
        this.x = x;
    }

    Zp64VoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(zp64, x.length);
            senderOutput = sender.send(x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
