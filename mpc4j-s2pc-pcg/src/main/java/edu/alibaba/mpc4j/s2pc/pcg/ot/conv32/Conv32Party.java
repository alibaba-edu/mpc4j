package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * F_3 -> F_2 modulus conversion party.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public interface Conv32Party extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param expectNum expect num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int expectNum) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param wi shares in F_3.
     * @return party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] conv(byte[] wi) throws MpcAbortException;
}
