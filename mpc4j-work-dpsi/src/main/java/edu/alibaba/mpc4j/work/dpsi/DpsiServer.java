package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * DPSI server.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public interface DpsiServer<T> extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
