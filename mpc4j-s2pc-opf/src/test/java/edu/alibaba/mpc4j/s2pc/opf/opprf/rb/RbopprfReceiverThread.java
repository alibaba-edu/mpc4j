package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Related-Batch OPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
class RbopprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final RbopprfReceiver receiver;
    /**
     * l bit length
     */
    private final int l;
    /**
     * input array
     */
    private final byte[][] inputArray;
    /**
     * point num
     */
    private final int pointNum;
    /**
     * the PRF outputs
     */
    private byte[][][] targetArray;

    RbopprfReceiverThread(RbopprfReceiver receiver, int l, byte[][] inputArray, int pointNum) {
        this.receiver = receiver;
        this.l = l;
        this.inputArray = inputArray;
        this.pointNum = pointNum;
    }

    byte[][][] getTargetArray() {
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