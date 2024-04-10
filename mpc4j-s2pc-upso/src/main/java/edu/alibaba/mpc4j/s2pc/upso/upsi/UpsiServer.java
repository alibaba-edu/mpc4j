package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.io.IOException;
import java.util.Set;

/**
 * UPSI server interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public interface UpsiServer<T> extends TwoPartyPto {
    /**
     * server initializes the protocol.
     *
     * @param upsiParams UPSI params.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(UpsiParams upsiParams) throws MpcAbortException;

    /**
     * server initializes the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize) throws MpcAbortException;

    /**
     * server executes the protocol.
     *
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @throws MpcAbortException the protocol failure aborts.
     * @throws IOException if I/O operations failed.
     */
    void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException, IOException;
}
