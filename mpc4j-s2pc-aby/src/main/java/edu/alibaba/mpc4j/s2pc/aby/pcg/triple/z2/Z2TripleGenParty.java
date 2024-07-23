package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;

/**
 * Z2 triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public interface Z2TripleGenParty extends MultiPartyPto {
    /**
     * inits the protocol.
     *
     * @param expectTotalNum expect total num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int expectTotalNum) throws MpcAbortException;

    /**
     * inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * executes the protocol.
     *
     * @param num num.
     * @return party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Z2Triple generate(int num) throws MpcAbortException;
}
