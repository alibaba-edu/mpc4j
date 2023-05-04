package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.Gf2eVoleSenderOutput;

/**
 * GF2E-core-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Gf2eCoreVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param gf2e the GF2E instance.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Gf2e gf2e, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param x x.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2eVoleSenderOutput send(byte[][] x) throws MpcAbortException;
}
