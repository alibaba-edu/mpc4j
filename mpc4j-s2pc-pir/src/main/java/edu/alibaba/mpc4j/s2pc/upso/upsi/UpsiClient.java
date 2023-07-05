package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * UPSI client interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public interface UpsiClient<T> extends TwoPartyPto {
    /**
     * Client initializes the protocol.
     *
     * @param upsiParams UPSI params.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(UpsiParams upsiParams) throws MpcAbortException;

    /**
     * Client initializes the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param clientElementSet the client element set.
     * @return intersection set.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> clientElementSet) throws MpcAbortException;
}
