package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Z2 multiplication triple generation party.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public interface Z2MtgParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param updateNum update num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int updateNum) throws MpcAbortException;

    /**
     * executes the protocol.
     *
     * @param num num.
     * @return party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Z2Triple generate(int num) throws MpcAbortException;
}
