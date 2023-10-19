package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Millionaire protocol party thread.
 *
 * @author Li Peng
 * @date 2023/5/11
 */
class MillionairePartyThread extends Thread {
    /**
     * the party
     */
    private final MillionaireParty party;
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

    MillionairePartyThread(MillionaireParty party, int l, byte[][] inputs) {
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
            party.getRpc().synchronize();
            party.init(l, num);
            party.getRpc().reset();
            party.getRpc().synchronize();
            zi = party.lt(l, inputs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}