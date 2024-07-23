package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Zl boolean to arithmetic protocol party thread.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
class ZlB2aPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlB2aParty party;
    /**
     * zl
     */
    private final Zl zl;
    /**
     * inputs
     */
    private final SquareZ2Vector inputs;
    /**
     * outputs
     */
    private MpcZlVector outputs;
    /**
     * num
     */
    private final int num;

    ZlB2aPartyThread(ZlB2aParty party, Zl zl, SquareZ2Vector inputs) {
        this.party = party;
        this.zl = zl;
        this.inputs = inputs;
        num = inputs.getNum();
    }

    MpcZlVector getZi() {
        return outputs;
    }

    @Override
    public void run() {
        try {
            party.getRpc().synchronize();
            party.init(zl.getL(), num);
            party.getRpc().reset();
            party.getRpc().synchronize();
            outputs = party.b2a(inputs, zl);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}