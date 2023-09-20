package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * GF2K-MSP-VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
class Gf2kMspVoleReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kMspVoleReceiver receiver;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * sparse num
     */
    private final int t;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed GF2K-VOLE receiver output
     */
    private final Gf2kVoleReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kMspVoleReceiverOutput receiverOutput;

    Gf2kMspVoleReceiverThread(Gf2kMspVoleReceiver receiver, byte[] delta, int t, int num) {
        this(receiver, delta, t, num, null);
    }

    Gf2kMspVoleReceiverThread(Gf2kMspVoleReceiver receiver, byte[] delta, int t, int num,
                              Gf2kVoleReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.delta = delta;
        this.t = t;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kMspVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(delta, t, num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(t, num) : receiver.receive(t, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
