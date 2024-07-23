package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl max2 party thread.
 *
 * @author Li Peng
 * @date 2023/5/24
 */
class ZlMax2PartyThread extends Thread {
    /**
     * the party
     */
    private final ZlMax2Party party;
    /**
     * z2c party
     */
    private final Z2cParty z2cParty;
    /**
     * Zl
     */
    private final Zl zl;
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
     * z0
     */
    private SquareZlVector shareZ;

    ZlMax2PartyThread(ZlMax2Party party, Z2cParty z2cParty, Zl zl, SquareZlVector shareX, SquareZlVector shareY) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.zl = zl;
        this.shareX = shareX;
        this.shareY = shareY;
        this.num = shareX.getNum();
    }

    SquareZlVector getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            z2cParty.init(zl.getL() * num);
            party.init(zl.getL(), num);
            shareZ = party.max2(shareX, shareY);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
