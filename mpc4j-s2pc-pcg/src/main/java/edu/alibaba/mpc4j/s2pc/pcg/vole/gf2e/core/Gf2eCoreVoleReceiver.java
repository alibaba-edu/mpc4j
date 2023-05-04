package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.Gf2eVoleReceiverOutput;

/**
 * GF2E-core-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Gf2eCoreVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param gf2e the GF2E instance.
     * @param delta  Δ.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Gf2e gf2e, byte[] delta, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2eVoleReceiverOutput receive(int num) throws MpcAbortException;
}
