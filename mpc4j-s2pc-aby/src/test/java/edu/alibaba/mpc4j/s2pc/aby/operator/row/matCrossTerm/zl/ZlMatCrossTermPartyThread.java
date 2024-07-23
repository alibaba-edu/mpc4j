package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.ZlMatCrossTermParty;

/**
 * Zl Matrix Cross Term Multiplication protocol party thread.
 *
 * @author Liqiang Peng
 * @date 2024/6/12
 */
class ZlMatCrossTermPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlMatCrossTermParty party;
    /**
     * z2c party
     */
    private final Z2cParty z2cParty;
    /**
     * input
     */
    private final SquareZlVector input;
    /**
     * output
     */
    private SquareZlVector output;
    /**
     * d1
     */
    private final int d1;
    /**
     * d2
     */
    private final int d2;
    /**
     * d3
     */
    private final int d3;
    /**
     * m
     */
    private final int m;
    /**
     * n
     */
    private final int n;


    ZlMatCrossTermPartyThread(ZlMatCrossTermParty party, Z2cParty z2cParty, SquareZlVector input, int m, int n, int d1, int d2, int d3) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.input = input;
        this.m = m;
        this.n = n;
        this.d1 = d1;
        this.d2 = d2;
        this.d3 = d3;
    }

    SquareZlVector getZi() {
        return output;
    }

    @Override
    public void run() {
        try {
            z2cParty.init((m + n) * d1 * d2 * d3);
            party.init(m, n, d1, d2, d3);
            party.getRpc().reset();
            party.getRpc().synchronize();
            output = party.matCrossTerm(input, d1, d2, d3, m, n);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}