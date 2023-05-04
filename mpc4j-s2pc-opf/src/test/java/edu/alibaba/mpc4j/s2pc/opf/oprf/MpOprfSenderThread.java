package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * multi-query OPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class MpOprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final MpOprfSender sender;
    /**
     * the batch size
     */
    private final int batchSize;
    /**
     * the sender output
     */
    private MpOprfSenderOutput senderOutput;

    MpOprfSenderThread(MpOprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    MpOprfSenderOutput getSenderOutput() {
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
