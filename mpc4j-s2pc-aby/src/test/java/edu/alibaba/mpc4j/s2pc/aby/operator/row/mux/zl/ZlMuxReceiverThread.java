package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Zl mux receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class ZlMuxReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final ZlMuxParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector shareX1;
    /**
     * y1
     */
    private final SquareZlVector shareY1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private SquareZlVector shareZ1;

    ZlMuxReceiverThread(ZlMuxParty receiver, SquareZ2Vector shareX1, SquareZlVector shareY1) {
        this.receiver = receiver;
        this.shareX1 = shareX1;
        this.shareY1 = shareY1;
        num = shareX1.getNum();
    }

    SquareZlVector getShareZ1() {
        return shareZ1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            shareZ1 = receiver.mux(shareX1, shareY1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
