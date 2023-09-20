package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * private (distinct) set membership sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class PdsmSenderThread extends Thread {
    /**
     * the sender
     */
    private final PdsmSender sender;
    /**
     * l
     */
    private final int l;
    /**
     * d
     */
    private final int d;
    /**
     * input arrays
     */
    private final byte[][][] inputArrays;
    /**
     * num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZ2Vector z0;

    PdsmSenderThread(PdsmSender sender, int l, int d, byte[][][] inputArray) {
        this.sender = sender;
        this.l = l;
        this.d = d;
        this.inputArrays = inputArray;
        num = inputArray.length;
    }

    SquareZ2Vector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(l, d, num);
            sender.getRpc().reset();
            z0 = sender.pdsm(l, inputArrays);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
