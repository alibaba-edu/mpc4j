package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-SSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kSspVodeReceiver extends TwoPartyPto {
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
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kSspVodeReceiverOutput receive(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num               num.
     * @param preReceiverOutput pre-computed receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kSspVodeReceiverOutput receive(int num, Gf2kVodeReceiverOutput preReceiverOutput) throws MpcAbortException;
}
