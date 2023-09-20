package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
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
    private final Gf2kVoleReceiverOutput receiverOutput;

    Gf2kNcVoleReceiverThread(Gf2kNcVoleReceiver receiver, byte[] delta, int num, int round) {
        this.receiver = receiver;
        this.delta = delta;
        this.num = num;
        this.round = round;
        receiverOutput = Gf2kVoleReceiverOutput.createEmpty(delta);
    }

    Gf2kVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(delta, num);
            for (int index = 0; index < round; index++) {
                receiverOutput.merge(receiver.receive());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
