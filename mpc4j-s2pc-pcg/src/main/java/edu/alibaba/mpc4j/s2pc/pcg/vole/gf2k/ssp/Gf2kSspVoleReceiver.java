package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * Single single-point GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface Gf2kSspVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta  Δ.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol
     *
     * @param num num.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kSspVoleReceiverOutput receive(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num               num.
     * @param preReceiverOutput pre-computed GF2K-VOLE receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kSspVoleReceiverOutput receive(int num, Gf2kVoleReceiverOutput preReceiverOutput) throws MpcAbortException;
}
