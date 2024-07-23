package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;

/**
 * Zl64 triple generation party thread.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
class Zl64TripleGenPartyThread extends Thread {
    /**
     * party
     */
    private final Zl64TripleGenParty party;
    /**
     * maxL
     */
    private final int maxL;
    /**
     * Zl64
     */
    private final Zl64 zl64;
    /**
     * num
     */
    private final int num;
    /**
     * first triple
     */
    private Zl64Triple firstTriple;
    /**
     * second triple
     */
    private Zl64Triple secondTriple;

    Zl64TripleGenPartyThread(Zl64TripleGenParty party, int maxL, Zl64 zl64, int num) {
        this.party = party;
        this.maxL = maxL;
        this.zl64 = zl64;
        this.num = num;
    }

    Zl64Triple getFirstTriple() {
        return firstTriple;
    }

    Zl64Triple getSecondTriple() {
        return secondTriple;
    }

    @Override
    public void run() {
        try {
            party.init(maxL, (1 << 14) / zl64.getL());
            firstTriple = party.generate(zl64, num);
            secondTriple = party.generate(zl64, num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
