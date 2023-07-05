package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;

/**
 * aid PSI aider.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface AidPsiAider extends ThreePartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psi(int serverElementSize, int clientElementSize) throws MpcAbortException;
}
