package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmSender;

/**
 * private (equal) set membership sender thread.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
class PesmSenderThread extends Thread {
    /**
     * the sender
     */
    private final PesmSender sender;
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

    PesmSenderThread(PesmSender sender, int l, int d, byte[][][] inputArray) {
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
            z0 = sender.pesm(l, inputArrays);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
