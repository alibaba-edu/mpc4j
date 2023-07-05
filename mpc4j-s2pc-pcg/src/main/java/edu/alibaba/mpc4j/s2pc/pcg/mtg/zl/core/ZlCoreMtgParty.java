package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;

/**
 * Zl core multiplication triple generator.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlCoreMtgParty extends MultiPartyPto {
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
     * @return Zl multiplication triple.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ZlTriple generate(int num) throws MpcAbortException;
}
