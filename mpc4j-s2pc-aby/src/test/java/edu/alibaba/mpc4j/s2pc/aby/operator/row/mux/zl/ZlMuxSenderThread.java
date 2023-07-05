package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Zl mux sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class ZlMuxSenderThread extends Thread {
    /**
     * the sender
     */
    private final ZlMuxParty sender;
    /**
     * x0
     */
    private final SquareZ2Vector x0;
    /**
     * y0
     */
    private final SquareZlVector y0;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZlVector shareZ0;

    ZlMuxSenderThread(ZlMuxParty sender, SquareZ2Vector shareX0, SquareZlVector shareY0) {
        this.sender = sender;
        this.x0 = shareX0;
        this.y0 = shareY0;
        num = shareX0.getNum();
    }

    SquareZlVector getShareZ0() {
        return shareZ0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            shareZ0 = sender.mux(x0, y0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
