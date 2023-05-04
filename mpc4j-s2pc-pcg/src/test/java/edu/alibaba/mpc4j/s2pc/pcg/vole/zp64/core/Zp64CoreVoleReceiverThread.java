package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;

/**
 * ZP64-core VOLE receiver thread.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
class Zp64CoreVoleReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final Zp64CoreVoleReceiver receiver;
    /**
     * the Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * Δ
     */
    private final long delta;
    /**
     * num
     */
    private final int num;
    /**
     * the receiver output
     */
    private Zp64VoleReceiverOutput receiverOutput;

    Zp64CoreVoleReceiverThread(Zp64CoreVoleReceiver receiver, Zp64 zp64, long delta, int num) {
        this.receiver = receiver;
        this.zp64 = zp64;
        this.delta = delta;
        this.num = num;
    }

    Zp64VoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(zp64, delta, num);
            receiverOutput = receiver.receive(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
