package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * batch-point RDPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class BpRdpprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final BpRdpprfReceiver receiver;
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
    private BpRdpprfReceiverOutput receiverOutput;

    BpRdpprfReceiverThread(BpRdpprfReceiver receiver, int[] alphaArray, int eachNum) {
        this(receiver, alphaArray, eachNum, null);
    }

    BpRdpprfReceiverThread(BpRdpprfReceiver receiver, int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.eachNum = eachNum;
        this.preReceiverOutput = preReceiverOutput;
    }

    BpRdpprfReceiverOutput getReceiverOutput() {
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
