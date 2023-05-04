package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * unbalanced related-batch OPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
class UrbopprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final UrbopprfSender sender;
    /**
     * the input / output bit length
     */
    private final int l;
    /**
     * input arrays
     */
    private final byte[][][] inputArrays;
    /**
     * target arrays
     */
    private final byte[][][] targetArrays;

    UrbopprfSenderThread(UrbopprfSender sender, int l, byte[][][] inputArrays, byte[][][] targetArrays) {
        this.sender = sender;
        this.l = l;
        this.inputArrays = inputArrays;
        this.targetArrays = targetArrays;
    }

    @Override
    public void run() {
        try {
            sender.init(l, inputArrays, targetArrays);
            sender.opprf();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
