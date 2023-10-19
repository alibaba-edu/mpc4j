package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * Payable PSI server interface.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public interface PayablePsiServer<T> extends TwoPartyPto {
    /**
     * server initializes protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * server executes protocol.
     *
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @return intersection set size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    int psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
