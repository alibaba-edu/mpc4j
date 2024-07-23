package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl DReLU Party Thread.
 *
 * @author Li Peng
 * @date 2023/5/23
 */
public class ZlDreluPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlDreluParty party;
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
    private SquareZ2Vector shareZ;

    ZlDreluPartyThread(ZlDreluParty party, Z2cParty z2cParty, SquareZlVector shareX) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.shareX = shareX;
        this.num = shareX.getNum();
    }

    SquareZ2Vector getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            z2cParty.init(shareX.getZl().getL() * num);
            party.init(shareX.getZl().getL(), num);
            party.getRpc().reset();
            party.getRpc().synchronize();
            shareZ = party.drelu(shareX);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
