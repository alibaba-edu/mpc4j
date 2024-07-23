package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.ZlDaBitTuple;

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
     * Zl
     */
    private final Zl zl;
    /**
     * num
     */
    private final int num;
    /**
     * output
     */
    private ZlDaBitTuple output;

    ZlDaBitGenPartyThread(ZlDaBitGenParty party, Zl zl, int num) {
        this.party = party;
        this.zl = zl;
        this.num = num;
    }

    ZlDaBitTuple getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.init(zl.getL(), num);
            output = party.generate(zl, num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
