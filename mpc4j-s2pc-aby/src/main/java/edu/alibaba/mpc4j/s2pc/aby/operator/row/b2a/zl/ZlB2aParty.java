package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl boolean to arithmetic protocol party.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public interface ZlB2aParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxL   max l.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xi the party's inputs.
     * @param zl zl.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector b2a(MpcZ2Vector xi, Zl zl) throws MpcAbortException;
}
