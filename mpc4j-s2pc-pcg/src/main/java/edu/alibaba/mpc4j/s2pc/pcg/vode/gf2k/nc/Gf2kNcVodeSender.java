package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-NC-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public interface Gf2kNcVodeSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param subfieldL subfield L.
     * @param num num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int subfieldL, int num) throws MpcAbortException;

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
    Gf2kVodeSenderOutput send() throws MpcAbortException;
}
