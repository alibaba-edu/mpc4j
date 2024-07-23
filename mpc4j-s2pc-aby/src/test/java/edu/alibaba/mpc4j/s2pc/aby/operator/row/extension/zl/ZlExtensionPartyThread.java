package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl value extension protocol party thread.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
class ZlExtensionPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlExtensionParty party;
    /**
     * z2c party
     */
    private final Z2cParty z2cParty;
    /**
     * input l
     */
    private final int inputL;
    /**
     * output l
     */
    private final int outputL;
    /**
     * xs
     */
    private final SquareZlVector inputs;
    /**
     * num
     */
    private final int num;
    /**
     * zi
     */
    private SquareZlVector zi;

    ZlExtensionPartyThread(ZlExtensionParty party, Z2cParty z2cParty, int inputL, int outputL, SquareZlVector inputs) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.inputL = inputL;
        this.outputL = outputL;
        this.inputs = inputs;
        num = inputs.getNum();
    }

    SquareZlVector getZi() {
        return zi;
    }

    @Override
    public void run() {
        try {
            z2cParty.init((inputL + outputL) * num);
            party.init(inputL, outputL, num);
            zi = party.zExtend(inputs, outputL, false);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}