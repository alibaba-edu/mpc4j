package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * Batched single-point GF2K-VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
class Gf2kBspVoleReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kBspVoleReceiver receiver;
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * batch num
     */
    private final int batchNum;
    /**
     * each num
     */
    private final int eachNum;
    /**
     * pre-computed receiver output
     */
    private final Gf2kVoleReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kBspVoleReceiverOutput receiverOutput;

    Gf2kBspVoleReceiverThread(Gf2kBspVoleReceiver receiver, Sgf2k field, byte[] delta, int batchNum, int eachNum) {
        this(receiver, field, delta, batchNum, eachNum, null);
    }

    Gf2kBspVoleReceiverThread(Gf2kBspVoleReceiver receiver, Sgf2k field, byte[] delta, int batchNum, int eachNum,
                              Gf2kVoleReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.batchNum = batchNum;
        this.eachNum = eachNum;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kBspVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(field.getSubfieldL(), delta);
            receiverOutput = receiver.receive(batchNum, eachNum, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
