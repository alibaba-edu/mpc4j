package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * no-choice GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public interface Gf2kNcVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param num num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kVoleSenderOutput send() throws MpcAbortException;
}
