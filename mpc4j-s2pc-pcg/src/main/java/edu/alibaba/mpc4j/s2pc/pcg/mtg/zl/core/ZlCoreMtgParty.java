package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;

/**
 * Zl core triple generation.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlCoreMtgParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the Zl triple.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ZlTriple generate(int num) throws MpcAbortException;
}
