package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.math.BigInteger;

/**
 * Zl Cross Term Multiplication Party.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
public interface ZlCrossTermParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxM max m.
     * @param maxN max n.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxM, int maxN) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param input input.
     * @param n     n.
     * @param m     m.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BigInteger crossTerm(BigInteger input, int m, int n) throws MpcAbortException;
}
