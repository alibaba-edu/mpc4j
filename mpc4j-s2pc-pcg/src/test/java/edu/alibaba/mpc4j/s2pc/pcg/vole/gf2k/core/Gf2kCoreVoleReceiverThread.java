package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * GF2K-core VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
class Gf2kCoreVoleReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final Gf2kCoreVoleReceiver receiver;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * num
     */
    private final int num;
    /**
     * the receiver output
     */
    private Gf2kVoleReceiverOutput receiverOutput;

    Gf2kCoreVoleReceiverThread(Gf2kCoreVoleReceiver receiver, byte[] delta, int num) {
        this.receiver = receiver;
        this.delta = delta;
        this.num = num;
    }

    Gf2kVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(delta, num);
            receiverOutput = receiver.receive(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
