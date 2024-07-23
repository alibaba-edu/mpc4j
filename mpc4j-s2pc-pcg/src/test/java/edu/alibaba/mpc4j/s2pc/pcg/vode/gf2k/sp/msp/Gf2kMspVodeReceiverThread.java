package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-MSP-VODE receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gf2kMspVodeReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kMspVodeReceiver receiver;
    /**
     * field
     */
    private final Dgf2k field;
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
     * pre-computed receiver output
     */
    private final Gf2kVodeReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kMspVodeReceiverOutput receiverOutput;

    Gf2kMspVodeReceiverThread(Gf2kMspVodeReceiver receiver, Dgf2k field, byte[] delta, int t, int num) {
        this(receiver, field, delta, t, num, null);
    }

    Gf2kMspVodeReceiverThread(Gf2kMspVodeReceiver receiver, Dgf2k field, byte[] delta, int t, int num,
                              Gf2kVodeReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.t = t;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kMspVodeReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(field.getSubfieldL(), delta);
            receiverOutput = receiver.receive(t, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
