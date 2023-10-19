package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl Truncation Party Thread.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class ZlTruncPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlTruncParty party;
    /**
     * x
     */
    private final SquareZlVector shareX;
    /**
     * l
     */
    private final int l;
    /**
     * num
     */
    private final int num;
    /**
     * z
     */
    private SquareZlVector shareZ;
    /**
     * s
     */
    private final int s;

    ZlTruncPartyThread(ZlTruncParty party, int l, SquareZlVector shareX, int s) {
        this.party = party;
        this.shareX = shareX;
        this.num = shareX.getNum();
        this.l = l;
        this.s = s;
    }

    SquareZlVector getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            party.init(l, num);
            shareZ = party.trunc(shareX, s);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
