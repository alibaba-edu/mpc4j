package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;

/**
 * Zp64-core VOLE receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public interface Zp64CoreVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param zp64   the Zp64 instance.
     * @param delta  Δ.
     * @param maxNum nax num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Zp64 zp64, long delta, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Zp64VoleReceiverOutput receive(int num) throws MpcAbortException;
}
