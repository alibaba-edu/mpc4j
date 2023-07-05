package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * client-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public interface CcpsiClient<T> extends TwoPartyPto {
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
     * @return the client output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CcpsiClientOutput<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
