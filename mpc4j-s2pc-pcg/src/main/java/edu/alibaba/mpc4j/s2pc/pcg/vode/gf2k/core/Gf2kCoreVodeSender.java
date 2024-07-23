package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-core-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public interface Gf2kCoreVodeSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param subfieldL subfield L.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int subfieldL) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xs x array.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kVodeSenderOutput send(byte[][] xs) throws MpcAbortException;
}
