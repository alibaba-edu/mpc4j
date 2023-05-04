package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * GF2K-core-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public interface Gf2kCoreVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta  Δ.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kVoleReceiverOutput receive(int num) throws MpcAbortException;
}
