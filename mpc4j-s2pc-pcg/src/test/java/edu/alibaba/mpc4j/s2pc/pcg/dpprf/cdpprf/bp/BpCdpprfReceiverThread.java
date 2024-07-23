package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Batched single-point COT receiver thread.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
class BpCdpprfReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BpCdpprfReceiver receiver;
    /**
     * α array
     */
    private final int[] alphaArray;
    /**
     * each num
     */
    private final int eachNum;
    /**
     * pre-computed receiver output
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private BpCdpprfReceiverOutput receiverOutput;

    BpCdpprfReceiverThread(BpCdpprfReceiver receiver, int[] alphaArray, int eachNum) {
        this(receiver, alphaArray, eachNum, null);
    }

    BpCdpprfReceiverThread(BpCdpprfReceiver receiver, int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.eachNum = eachNum;
        this.preReceiverOutput = preReceiverOutput;
    }

    BpCdpprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.puncture(alphaArray, eachNum, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
