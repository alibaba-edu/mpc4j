package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * server-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface ScpsiServer<T> extends TwoPartyPto {
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
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ScpsiServerOutput<T> psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
