package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
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
     * z2c party
     */
    private final Z2cParty z2cParty;
    /**
     * x
     */
    private final SquareZlVector shareX;
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

    ZlTruncPartyThread(ZlTruncParty party, Z2cParty z2cParty, SquareZlVector shareX, int s) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.shareX = shareX;
        this.num = shareX.getNum();
        this.s = s;
    }

    SquareZlVector getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            z2cParty.init(shareX.getZl().getL() * num);
            party.init(shareX.getZl().getL(), num);
            shareZ = party.trunc(shareX, s);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
