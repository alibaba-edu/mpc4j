package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.math.BigInteger;

/**
 * Zl Cross Term Multiplication protocol party thread.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
class ZlCrossTermPartyThread extends Thread {
    /**
     * the party
     */
    private final ZlCrossTermParty party;
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
     * num
     */
    private final int num;
    /**
     * m
     */
    private final int m;
    /**
     * n
     */
    private final int n;

    ZlCrossTermPartyThread(ZlCrossTermParty party, Z2cParty z2cParty, SquareZlVector input, int m, int n) {
        this.party = party;
        this.z2cParty = z2cParty;
        this.input = input;
        num = input.getNum();
        this.m = m;
        this.n = n;
    }

    SquareZlVector getZi() {
        return output;
    }

    @Override
    public void run() {
        try {
            BigInteger[] result = new BigInteger[num];
            z2cParty.init(m * n);
            party.init(m, n);
            party.getRpc().reset();
            party.getRpc().synchronize();
            for (int i = 0; i < num; i++) {
                result[i] = party.crossTerm(input.getZlVector().getElement(i), m, n);
            }
            output = SquareZlVector.create(ZlFactory.createInstance(party.getEnvType(), m + n), result, false);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}