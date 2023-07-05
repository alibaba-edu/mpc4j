package edu.alibaba.mpc4j.s2pc.pso.psica;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI Cardinality client.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public interface PsiCaClient<T> extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementSet  client element set.
     * @param serverElementSize server element size.
     * @return cardinality of the intersection.
     * @throws MpcAbortException the protocol failure aborts.
     */
    int psiCardinality(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
