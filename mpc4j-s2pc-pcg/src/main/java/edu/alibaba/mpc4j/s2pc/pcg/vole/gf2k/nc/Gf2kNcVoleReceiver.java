package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * no-choice GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public interface Gf2kNcVoleReceiver extends TwoPartyPto {
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
    Gf2kVoleReceiverOutput receive() throws MpcAbortException;
}
