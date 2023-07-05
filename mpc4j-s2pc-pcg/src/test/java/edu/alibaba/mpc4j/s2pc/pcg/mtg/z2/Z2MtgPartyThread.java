package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Z2 multiplication triple generator party thread.
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
class Z2MtgPartyThread extends Thread {
    /**
     * party
     */
    private final Z2MtgParty party;
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
    private Z2Triple output;

    Z2MtgPartyThread(Z2MtgParty party, int num) {
        this(party, num, num);
    }

    Z2MtgPartyThread(Z2MtgParty party, int num, int updateNum) {
        this.party = party;
        this.num = num;
        this.updateNum = updateNum;
    }

    Z2Triple getOutput() {
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
