package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * GF2K-NC-VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
class Gf2kNcVoleReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Gf2kNcVoleReceiver receiver;
    /**
     * field
     */
    private final Sgf2k field;
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
    private Gf2kVoleReceiverOutput receiverOutput;

    Gf2kNcVoleReceiverThread(Gf2kNcVoleReceiver receiver, Sgf2k field, byte[] delta, int num, int round) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.num = num;
        this.round = round;
    }

    Gf2kVoleReceiverOutput getReceiverOutput() {
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
