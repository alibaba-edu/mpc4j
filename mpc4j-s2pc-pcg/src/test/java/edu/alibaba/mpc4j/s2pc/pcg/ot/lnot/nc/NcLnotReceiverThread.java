package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

/**
 * no-choice 1-out-of-n (with n = 2^l) receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class NcLnotReceiverThread extends Thread {
    /**
     * receiver
     */
    private final NcLnotReceiver receiver;
    /**
     * l
     */
    private final int l;
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
    private final LnotReceiverOutput receiverOutput;

    NcLnotReceiverThread(NcLnotReceiver receiver, int l, int num, int round) {
        this.receiver = receiver;
        this.l = l;
        this.num = num;
        this.round = round;
        receiverOutput = LnotReceiverOutput.createEmpty(l);
    }

    LnotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, num);
            for (int index = 0; index < round; index++) {
                receiverOutput.merge(receiver.receive());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}