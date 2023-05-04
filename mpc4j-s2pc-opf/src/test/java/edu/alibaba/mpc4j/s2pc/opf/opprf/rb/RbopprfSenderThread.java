package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Related-Batch OPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
class RbopprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final RbopprfSender sender;
    /**
     * l bit length
     */
    private final int l;
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * point num
     */
    private final int pointNum;
    /**
     * input arrays
     */
    private final byte[][][] inputArrays;
    /**
     * target arrays
     */
    private final byte[][][] targetArrays;

    RbopprfSenderThread(RbopprfSender sender, int l, byte[][][] inputArrays, byte[][][] targetArrays) {
        this.sender = sender;
        this.l = l;
        batchSize = inputArrays.length;
        pointNum = Arrays.stream(inputArrays)
            .mapToInt(inputArray -> inputArray.length)
            .sum();
        this.inputArrays = inputArrays;
        this.targetArrays = targetArrays;
    }

    @Override
    public void run() {
        try {
            sender.init(batchSize, pointNum);
            sender.opprf(l, inputArrays, targetArrays);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
