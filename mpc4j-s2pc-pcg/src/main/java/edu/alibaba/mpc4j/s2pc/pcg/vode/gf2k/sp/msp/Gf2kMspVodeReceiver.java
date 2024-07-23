package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * GF2K-MSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kMspVodeReceiver extends TwoPartyPto {
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
     * @param t   sparse num.
     * @param num num.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kMspVodeReceiverOutput receive(int t, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t                 sparse num.
     * @param num               num.
     * @param preReceiverOutput pre-computed sender output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kMspVodeReceiverOutput receive(int t, int num, Gf2kVodeReceiverOutput preReceiverOutput) throws MpcAbortException;
}
