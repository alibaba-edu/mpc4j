package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl corr Party.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public interface ZlCorrParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max l.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xi the arithmetic share xi.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector corr(SquareZlVector xi) throws MpcAbortException;
}
