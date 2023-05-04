package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * no-choice COT receiver thread.
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class NcCotReceiverThread extends Thread {
    /**
     * receiver
     */
    private final NcCotReceiver receiver;
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
    private final CotReceiverOutput receiverOutput;

    NcCotReceiverThread(NcCotReceiver receiver, int num, int round) {
        this.receiver = receiver;
        this.num = num;
        this.round = round;
        receiverOutput = CotReceiverOutput.createEmpty();
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            for (int index = 0; index < round; index++) {
                receiverOutput.merge(receiver.receive());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}