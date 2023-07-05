package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Zl multiplication triple generator party thread.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
class ZlMtgPartyThread extends Thread {
    /**
     * party
     */
    private final ZlMtgParty party;
    /**
     * num
     */
    private final int num;
    /**
     * update num
     */
    private final int updateNum;
    /**
     * output
     */
    private ZlTriple output;

    ZlMtgPartyThread(ZlMtgParty party, int num) {
        this(party, num, num);
    }

    ZlMtgPartyThread(ZlMtgParty party, int num, int updateNum) {
        this.party = party;
        this.num = num;
        this.updateNum = updateNum;
    }

    ZlTriple getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.init(updateNum);
            output = party.generate(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
