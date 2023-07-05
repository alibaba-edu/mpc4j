package edu.alibaba.mpc4j.s2pc.pso.psica;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI Cardinality server.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public interface PsiCaServer<T> extends TwoPartyPto {
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
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psiCardinality(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
