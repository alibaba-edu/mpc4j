package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;

/**
 * Z2 triple generation party thread.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
class Z2TripleGenPartyThread extends Thread {
    /**
     * party
     */
    private final Z2TripleGenParty party;
    /**
     * num
     */
    private final int num;
    /**
     * first triple
     */
    private Z2Triple firstTriple;
    /**
     * second triple
     */
    private Z2Triple secondTriple;

    Z2TripleGenPartyThread(Z2TripleGenParty party, int num) {
        this.party = party;
        this.num = num;
    }

    Z2Triple getFirstTriple() {
        return firstTriple;
    }

    Z2Triple getSecondTriple() {
        return secondTriple;
    }

    @Override
    public void run() {
        try {
            party.init(1 << 14);
            firstTriple = party.generate(num);
            secondTriple = party.generate(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
