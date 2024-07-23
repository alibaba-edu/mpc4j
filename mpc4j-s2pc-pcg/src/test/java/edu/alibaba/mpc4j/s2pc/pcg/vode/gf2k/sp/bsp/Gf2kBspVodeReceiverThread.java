package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-BSP-VODE receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gf2kBspVodeReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kBspVodeReceiver receiver;
    /**
     * field
     */
    private final Dgf2k field;
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
    private final Gf2kVodeReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kBspVodeReceiverOutput receiverOutput;

    Gf2kBspVodeReceiverThread(Gf2kBspVodeReceiver receiver, Dgf2k field, byte[] delta, int batchNum, int eachNum) {
        this(receiver, field, delta, batchNum, eachNum, null);
    }

    Gf2kBspVodeReceiverThread(Gf2kBspVodeReceiver receiver, Dgf2k field, byte[] delta, int batchNum, int eachNum,
                              Gf2kVodeReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.batchNum = batchNum;
        this.eachNum = eachNum;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kBspVodeReceiverOutput getReceiverOutput() {
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
