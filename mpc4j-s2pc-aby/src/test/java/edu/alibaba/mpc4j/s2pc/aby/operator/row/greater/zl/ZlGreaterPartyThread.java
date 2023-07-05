package edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl greater party thread.
 *
 * @author Li Peng
 * @date 2023/5/24
 */
class ZlGreaterPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlGreaterParty party;
    /**
     * x
     */
    private final SquareZlVector shareX;
    /**
     * y
     */
    private final SquareZlVector shareY;
    /**
     * num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z0
     */
    private SquareZlVector shareZ;

    ZlGreaterPartyThread(ZlGreaterParty party, SquareZlVector shareX, SquareZlVector shareY) {
        this.party = party;
        this.shareX = shareX;
        this.shareY = shareY;
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
            shareZ = party.gt(shareX, shareY);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
