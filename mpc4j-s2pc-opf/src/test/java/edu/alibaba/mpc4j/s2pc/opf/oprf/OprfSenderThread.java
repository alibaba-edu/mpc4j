package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class OprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final OprfSender sender;
    /**
     * the batch size
     */
    private final int batchSize;
    /**
     * the sender output
     */
    private OprfSenderOutput senderOutput;

    OprfSenderThread(OprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    OprfSenderOutput getSenderOutput() {
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