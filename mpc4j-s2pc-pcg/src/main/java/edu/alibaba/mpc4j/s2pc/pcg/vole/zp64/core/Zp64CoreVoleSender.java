package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;

/**
 * Zp64-core VOLE sender.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public interface Zp64CoreVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param zp64   the Zp64 instance.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Zp64 zp64, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param x x.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Zp64VoleSenderOutput send(long[] x) throws MpcAbortException;
}
