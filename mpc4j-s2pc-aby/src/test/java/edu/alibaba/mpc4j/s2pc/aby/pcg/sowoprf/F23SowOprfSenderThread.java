package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * (F2, F3)-sowOPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2024/10/24
 */
class F23SowOprfSenderThread extends Thread {
    /**
     * sender
     */
    private final F23SowOprfSender sender;
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * pre-computed COT sender output
     */
    private final CotSenderOutput preCotSenderOutput;
    /**
     * sender output
     */
    private byte[][] senderOutput;

    F23SowOprfSenderThread(F23SowOprfSender sender, int batchSize, CotSenderOutput preCotSenderOutput) {
        this.sender = sender;
        this.batchSize = batchSize;
        this.preCotSenderOutput = preCotSenderOutput;
    }

    byte[][] getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(batchSize);
            senderOutput = sender.oprf(batchSize, preCotSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
