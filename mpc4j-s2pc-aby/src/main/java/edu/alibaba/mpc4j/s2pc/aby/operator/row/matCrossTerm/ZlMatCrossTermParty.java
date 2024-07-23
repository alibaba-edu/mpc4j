package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl Matrix Cross Term Multiplication Party.
 *
 * @author Liqiang Peng
 * @date 2024/6/7
 */
public interface ZlMatCrossTermParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxM  max m.
     * @param maxN  max n.
     * @param maxD1 max d1.
     * @param maxD2 max d2.
     * @param maxD3 max d3.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxM, int maxN, int maxD1, int maxD2, int maxD3) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param input input.
     * @param d1    d1.
     * @param d2    d2.
     * @param d3    d3.
     * @param m     m.
     * @param n     n.
     * @return the party's output, the share of X_m^(d1*d2) * Y_n^(d2*d3).
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector matCrossTerm(SquareZlVector input, int d1, int d2, int d3, int m, int n) throws MpcAbortException;
}
