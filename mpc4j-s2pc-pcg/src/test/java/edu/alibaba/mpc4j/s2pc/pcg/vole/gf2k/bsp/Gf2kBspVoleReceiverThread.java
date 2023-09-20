package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * Batched single-point GF2K-VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
class Gf2kBspVoleReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final Gf2kBspVoleReceiver receiver;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * batch num
     */
    private final int batchNum;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed receiver output
     */
    private final Gf2kVoleReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kBspVoleReceiverOutput receiverOutput;

    Gf2kBspVoleReceiverThread(Gf2kBspVoleReceiver receiver, byte[] delta, int batchNum, int num) {
        this(receiver, delta, batchNum, num, null);
    }

    Gf2kBspVoleReceiverThread(Gf2kBspVoleReceiver receiver, byte[] delta, int batchNum, int num,
                              Gf2kVoleReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.delta = delta;
        this.batchNum = batchNum;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kBspVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(delta, batchNum, num);
            receiverOutput = preReceiverOutput == null
                ? receiver.receive(batchNum, num) : receiver.receive(batchNum, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
