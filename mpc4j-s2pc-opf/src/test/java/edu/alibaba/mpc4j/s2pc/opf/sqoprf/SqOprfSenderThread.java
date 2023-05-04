package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * single-query OPRF sender thread.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final SqOprfSender sender;
    /**
     * the batch size
     */
    private final int batchSize;
    /**
     * the key
     */
    private SqOprfKey key;

    SqOprfSenderThread(SqOprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    SqOprfKey getKey() {
        return key;
    }

    @Override
    public void run() {
        try {
            key = sender.keyGen();
            sender.init(batchSize, key);
            sender.oprf(batchSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
