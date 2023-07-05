package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Zl edaBit generation party.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public interface ZlEdaBitGenParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxNum maximum number of generated edaBit.
     * @throws MpcAbortException the protocol is failure abort.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num number of generated edaBit.
     * @return edaBit vector.
     * @throws MpcAbortException the protocol if failure abort.
     */
    SquareZlEdaBitVector generate(int num) throws MpcAbortException;
}
