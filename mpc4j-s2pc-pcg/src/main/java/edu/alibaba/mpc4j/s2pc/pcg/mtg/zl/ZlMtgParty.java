package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Zl multiplication triple generator.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlMtgParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param updateNum update num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int updateNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return Zl multiplication triple.
     * @throws MpcAbortException the protocol failure aborts
     */
    ZlTriple generate(int num) throws MpcAbortException;
}
