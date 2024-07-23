package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * multi single-point GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public interface Gf2kMspVoleReceiver extends TwoPartyPto {
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
    Gf2kMspVoleReceiverOutput receive(int t, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t                 sparse num.
     * @param num               num.
     * @param preReceiverOutput pre-computed sender output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kMspVoleReceiverOutput receive(int t, int num, Gf2kVoleReceiverOutput preReceiverOutput) throws MpcAbortException;
}
