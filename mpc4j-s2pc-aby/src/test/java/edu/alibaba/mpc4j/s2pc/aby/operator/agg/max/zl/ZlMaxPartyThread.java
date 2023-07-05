package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl Max party thread.
 *
 * @author Li Peng
 * @date 2023/5/24
 */
class ZlMaxPartyThread extends Thread {
    /**
     * the sender
     */
    private final ZlMaxParty party;
    /**
     * x
     */
    private final SquareZlVector x;
    /**
     * num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z
     */
    private SquareZlVector shareZ;

    ZlMaxPartyThread(ZlMaxParty party, SquareZlVector shareX) {
        this.party = party;
        this.x = shareX;
        this.num = shareX.getNum();
        this.l = shareX.getZl().getL();
    }

    SquareZlVector getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            party.init(l, num);
            shareZ = party.max(x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
