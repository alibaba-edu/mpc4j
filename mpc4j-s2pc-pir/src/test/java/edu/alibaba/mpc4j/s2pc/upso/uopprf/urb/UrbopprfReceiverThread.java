package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * unbalanced related-batch OPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
class UrbopprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final UrbopprfReceiver receiver;
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

    UrbopprfReceiverThread(UrbopprfReceiver receiver, int l, byte[][] inputArray, int pointNum) {
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
            receiver.init(l, inputArray.length, pointNum);
            targetArray = receiver.opprf(inputArray);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}