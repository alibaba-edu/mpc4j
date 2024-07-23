package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl Max party thread.
 *
 * @author Li Peng
 * @date 2023/5/24
 */
class ZlMaxPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlMaxParty party;
    /**
     * zlc party
     */
    private final Z2cParty z2cParty;
    /**
     * Zl
     */
    private final Zl zl;
    /**
     * x
     */
    private final SquareZlVector x;
    /**
     * num
     */
    private final int num;
    /**
     * z
     */
    private SquareZlVector shareZ;

    ZlMaxPartyThread(ZlMaxParty party, Z2cParty z2cParty, Zl zl, SquareZlVector shareX) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.zl = zl;
        this.x = shareX;
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
            shareZ = party.max(x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
