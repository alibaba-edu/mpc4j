package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * private equality test sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class PeqtPartyThread extends Thread {
    /**
     * the party
     */
    private final PeqtParty party;
    /**
     * l
     */
    private final int l;
    /**
     * xs
     */
    private final byte[][] inputs;
    /**
     * num
     */
    private final int num;
    /**
     * zi
     */
    private SquareZ2Vector zi;

    PeqtPartyThread(PeqtParty party, int l, byte[][] inputs) {
        this.party = party;
        this.l = l;
        this.inputs = inputs;
        num = inputs.length;
    }

    SquareZ2Vector getZi() {
        return zi;
    }

    @Override
    public void run() {
        try {
            party.init(l, num);
            zi = party.peqt(l, inputs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
