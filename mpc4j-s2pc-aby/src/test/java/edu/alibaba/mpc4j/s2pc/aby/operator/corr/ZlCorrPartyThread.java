package edu.alibaba.mpc4j.s2pc.aby.operator.corr;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
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
    private SquareZlVector shareCorr;

    ZlCorrPartyThread(ZlCorrParty party, Z2cParty z2cParty, SquareZlVector shareX) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.shareX = shareX;
        this.num = shareX.getNum();
    }

    SquareZlVector getShareZ() {
        return shareCorr;
    }

    @Override
    public void run() {
        try {
            z2cParty.init(shareX.getZl().getL() * num);
            party.init(shareX.getZl().getL(), num);
            shareCorr = party.corr(shareX);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
