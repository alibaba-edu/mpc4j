package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmReceiver;

/**
 * private (equal) set membership receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class PesmReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PesmReceiver receiver;
    /**
     * l
     */
    private final int l;
    /**
     * d
     */
    private final int d;
    /**
     * input array
     */
    private final byte[][] inputArray;
    /**
     * num
     */
    private final int num;
    /**
     * z1
     */
    private SquareZ2Vector z1;

    PesmReceiverThread(PesmReceiver receiver, int l, int d, byte[][] inputArray) {
        this.receiver = receiver;
        this.l = l;
        this.d = d;
        this.inputArray = inputArray;
        num = inputArray.length;
    }

    SquareZ2Vector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, d, num);
            z1 = receiver.pesm(l, d, inputArray);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
