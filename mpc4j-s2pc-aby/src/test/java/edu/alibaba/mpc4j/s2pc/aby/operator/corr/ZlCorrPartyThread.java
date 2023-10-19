package edu.alibaba.mpc4j.s2pc.aby.operator.corr;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrParty;

/**
 * Zl Corr Party Thread.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class ZlCorrPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlCorrParty party;
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
    private SquareZlVector shareCorr;

    ZlCorrPartyThread(ZlCorrParty party, int l, SquareZlVector shareX) {
        this.party = party;
        this.shareX = shareX;
        this.num = shareX.getNum();
        this.l = l;
    }

    SquareZlVector getShareZ() {
        return shareCorr;
    }

    @Override
    public void run() {
        try {
            party.init(l, num);
            shareCorr = party.corr(shareX);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
