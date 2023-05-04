package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * unbalanced batched OPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
class UbopprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final UbopprfSender sender;
    /**
     * the input / output bit length
     */
    private final int l;
    /**
     * sender input arrays
     */
    private final byte[][][] inputArrays;
    /**
     * sender target arrays
     */
    private final byte[][][] targetArrays;

    UbopprfSenderThread(UbopprfSender sender, int l, byte[][][] inputArrays, byte[][][] targetArrays) {
        this.sender = sender;
        this.l = l;
        this.inputArrays = inputArrays;
        this.targetArrays = targetArrays;
    }

    @Override
    public void run() {
        try {
            sender.init(l, inputArrays, targetArrays);
            sender.getRpc().synchronize();
            sender.getRpc().reset();
            sender.opprf();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}