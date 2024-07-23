package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-core-VODE receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
class Gf2kCoreVodeReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kCoreVodeReceiver receiver;
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
     * the receiver output
     */
    private Gf2kVodeReceiverOutput receiverOutput;

    Gf2kCoreVodeReceiverThread(Gf2kCoreVodeReceiver receiver, Dgf2k field, byte[] delta, int num) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.num = num;
    }

    Gf2kVodeReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(field.getSubfieldL(), delta);
            receiverOutput = receiver.receive(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
