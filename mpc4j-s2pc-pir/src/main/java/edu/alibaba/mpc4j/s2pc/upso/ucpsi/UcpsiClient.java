package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * Unbalanced Circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public interface UcpsiClient<T> extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param serverElementSize    server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int serverElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementSet client element set.
     * @return the client output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    UcpsiClientOutput<T> psi(Set<T> clientElementSet) throws MpcAbortException;
}
