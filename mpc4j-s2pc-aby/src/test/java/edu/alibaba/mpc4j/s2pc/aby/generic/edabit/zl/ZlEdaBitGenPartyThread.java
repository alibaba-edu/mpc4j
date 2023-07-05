package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Zl edaBit generation party thread.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
class ZlEdaBitGenPartyThread extends Thread {
    /**
     * party
     */
    private final ZlEdaBitGenParty party;
    /**
     * num
     */
    private final int num;
    /**
     * output
     */
    private SquareZlEdaBitVector output;

    ZlEdaBitGenPartyThread(ZlEdaBitGenParty party, int num) {
        this.party = party;
        this.num = num;
    }

    SquareZlEdaBitVector getOutput() {
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
