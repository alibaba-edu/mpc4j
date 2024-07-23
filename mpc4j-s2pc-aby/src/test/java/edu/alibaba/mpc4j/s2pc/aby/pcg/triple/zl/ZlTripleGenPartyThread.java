package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.ZlTriple;

/**
 * Zl triple generation party thread.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class ZlTripleGenPartyThread extends Thread {
    /**
     * party
     */
    private final ZlTripleGenParty party;
    /**
     * maxL
     */
    private final int maxL;
    /**
     * Zl
     */
    private final Zl zl;
    /**
     * num
     */
    private final int num;
    /**
     * first triple
     */
    private ZlTriple firstTriple;
    /**
     * second triple
     */
    private ZlTriple secondTriple;

    ZlTripleGenPartyThread(ZlTripleGenParty party, int maxL, Zl zl, int num) {
        this.party = party;
        this.maxL = maxL;
        this.zl = zl;
        this.num = num;
    }

    ZlTriple getFirstTriple() {
        return firstTriple;
    }

    ZlTriple getSecondTriple() {
        return secondTriple;
    }

    @Override
    public void run() {
        try {
            party.init(maxL, (1 << 14) / zl.getL());
            firstTriple = party.generate(zl, num);
            secondTriple = party.generate(zl, num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
