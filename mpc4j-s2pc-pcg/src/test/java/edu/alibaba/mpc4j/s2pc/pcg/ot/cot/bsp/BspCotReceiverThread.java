package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Batched single-point COT receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class BspCotReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BspCotReceiver receiver;
    /**
     * α array
     */
    private final int[] alphaArray;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed receiver output
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private BspCotReceiverOutput receiverOutput;

    BspCotReceiverThread(BspCotReceiver receiver, int[] alphaArray, int num) {
        this(receiver, alphaArray, num, null);
    }

    BspCotReceiverThread(BspCotReceiver receiver, int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    BspCotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(alphaArray.length, num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(alphaArray, num) : receiver.receive(alphaArray, num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
