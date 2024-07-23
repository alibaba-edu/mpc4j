package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * Batched single-point GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public interface Gf2kBspVoleReceiver extends TwoPartyPto {
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
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleReceiverOutput receive(int batchNum, int eachNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum          batch num.
     * @param eachNum           each num.
     * @param preReceiverOutput pre-computed receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleReceiverOutput receive(int batchNum, int eachNum, Gf2kVoleReceiverOutput preReceiverOutput)
        throws MpcAbortException;
}
