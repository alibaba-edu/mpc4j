package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * GF2K-core-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public interface Gf2kCoreVoleSender extends TwoPartyPto {
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
     * @param xs x array.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kVoleSenderOutput send(byte[][] xs) throws MpcAbortException;
}
