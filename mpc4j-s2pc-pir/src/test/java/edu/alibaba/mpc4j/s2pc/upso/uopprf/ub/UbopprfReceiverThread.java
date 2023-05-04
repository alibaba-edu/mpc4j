package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * unbalanced batched OPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class UbopprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final UbopprfReceiver receiver;
    /**
     * the input / output bit length
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

    UbopprfReceiverThread(UbopprfReceiver receiver, int l, byte[][] inputArray, int pointNum) {
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
            receiver.init(l, inputArray.length, pointNum);
            receiver.getRpc().synchronize();
            receiver.getRpc().reset();
            targetArray = receiver.opprf(inputArray);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}