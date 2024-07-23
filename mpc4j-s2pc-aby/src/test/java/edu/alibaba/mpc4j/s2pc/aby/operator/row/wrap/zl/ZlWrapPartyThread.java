package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;

/**
 * Zl wrap protocol party thread.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
class ZlWrapPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlWrapParty party;
    /**
     * z2c party
     */
    private final Z2cParty z2cParty;
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

    ZlWrapPartyThread(ZlWrapParty party, Z2cParty z2cParty, int l, byte[][] inputs) {
        this.party = party;
        this.z2cParty = z2cParty;
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
            z2cParty.init(l * num);
            party.init(l, num);
            party.getRpc().reset();
            party.getRpc().synchronize();
            zi = party.wrap(l, inputs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}