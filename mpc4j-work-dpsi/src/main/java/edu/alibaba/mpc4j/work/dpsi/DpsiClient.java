package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * DPSI client.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public interface DpsiClient<T> extends TwoPartyPto {
    /**
     * Client initializes the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param clientElementSet  server element set.
     * @param serverElementSize client element size.
     * @return client output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
