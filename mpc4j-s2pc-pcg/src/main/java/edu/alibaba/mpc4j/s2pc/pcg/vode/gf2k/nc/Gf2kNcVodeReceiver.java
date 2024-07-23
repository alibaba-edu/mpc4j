package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-NC-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public interface Gf2kNcVodeReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param subfieldL subfield L.
     * @param delta     Δ.
     * @param num       num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int subfieldL, byte[] delta, int num) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @param delta Δ.
     * @param num   num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kVodeReceiverOutput receive() throws MpcAbortException;
}
