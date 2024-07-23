package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-NC-VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
class Gf2kNcVodeReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kNcVodeReceiver receiver;
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
     * round
     */
    private final int round;
    /**
     * the receiver output
     */
    private Gf2kVodeReceiverOutput receiverOutput;

    Gf2kNcVodeReceiverThread(Gf2kNcVodeReceiver receiver, Dgf2k field, byte[] delta, int num, int round) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.num = num;
        this.round = round;
    }

    Gf2kVodeReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(field.getSubfieldL(), delta, num);
            for (int index = 0; index < round; index++) {
                if (receiverOutput == null) {
                    receiverOutput = receiver.receive();
                } else {
                    receiverOutput.merge(receiver.receive());
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
