package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Zl daBit generation party thread.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
class ZlDaBitGenPartyThread extends Thread {
    /**
     * party
     */
    private final ZlDaBitGenParty party;
    /**
     * num
     */
    private final int num;
    /**
     * output
     */
    private SquareZlDaBitVector output;

    ZlDaBitGenPartyThread(ZlDaBitGenParty party, int num) {
        this.party = party;
        this.num = num;
    }

    SquareZlDaBitVector getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.init(num);
            output = party.generate(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
