package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
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
    private SquareZ2Vector shareZ;

    ZlDreluPartyThread(ZlDreluParty party, int l, SquareZlVector shareX) {
        this.party = party;
        this.shareX = shareX;
        this.num = shareX.getNum();
        this.l = l;
    }

    SquareZ2Vector getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            party.getRpc().synchronize();
            party.init(l, num);
            party.getRpc().reset();
            party.getRpc().synchronize();
            shareZ = party.drelu(shareX);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
