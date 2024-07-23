package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-core-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public interface Gf2kCoreVodeReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param subfieldL subfield L.
     * @param delta     Δ.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int subfieldL, byte[] delta) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kVodeReceiverOutput receive(int num) throws MpcAbortException;
}
