package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Zl daBit generation party.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public interface ZlDaBitGenParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxNum maximum number of generated daBit.
     * @throws MpcAbortException the protocol is failure abort.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num number of generated daBit.
     * @return daBit vector.
     * @throws MpcAbortException the protocol if failure abort.
     */
    SquareZlDaBitVector generate(int num) throws MpcAbortException;
}
