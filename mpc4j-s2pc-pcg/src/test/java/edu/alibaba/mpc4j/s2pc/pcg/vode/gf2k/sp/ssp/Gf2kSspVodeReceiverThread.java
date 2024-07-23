package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * Single single-point GF2K-VODE receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gf2kSspVodeReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final Gf2kSspVodeReceiver receiver;
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed receiver output
     */
    private final Gf2kVodeReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kSspVodeReceiverOutput receiverOutput;

    Gf2kSspVodeReceiverThread(Gf2kSspVodeReceiver receiver, Dgf2k field, byte[] delta, int num) {
        this(receiver, field, delta, num, null);
    }

    Gf2kSspVodeReceiverThread(Gf2kSspVodeReceiver receiver, Dgf2k field, byte[] delta, int num,
                              Gf2kVodeReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kSspVodeReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(field.getSubfieldL(), delta);
            receiverOutput = receiver.receive(num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
