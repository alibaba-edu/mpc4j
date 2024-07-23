package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * (F3, F2)-sowOPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class F32SowOprfSenderThread extends Thread {
    /**
     * sender
     */
    private final F32SowOprfSender sender;
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * sender output
     */
    private byte[][] senderOutput;

    F32SowOprfSenderThread(F32SowOprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    byte[][] getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(batchSize);
            senderOutput = sender.oprf(batchSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
