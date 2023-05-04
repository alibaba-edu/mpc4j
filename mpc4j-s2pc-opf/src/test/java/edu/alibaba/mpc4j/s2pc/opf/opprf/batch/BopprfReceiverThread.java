package edu.alibaba.mpc4j.s2pc.opf.opprf.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Batch OPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class BopprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final BopprfReceiver receiver;
    /**
     * l bit length
     */
    private final int l;
    /**
     * the batched receiver input array
     */
    private final byte[][] inputArray;
    /**
     * point num
     */
    private final int pointNum;
    /**
     * the PRF outputs
     */
    private byte[][] targetArray;

    BopprfReceiverThread(BopprfReceiver receiver, int l, byte[][] inputArray, int pointNum) {
        this.receiver = receiver;
        this.l = l;
        this.inputArray = inputArray;
        this.pointNum = pointNum;
    }

    byte[][] getTargetArray() {
        return targetArray;
    }

    @Override
    public void run() {
        try {
            receiver.init(inputArray.length, pointNum);
            targetArray = receiver.opprf(l, inputArray, pointNum);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}