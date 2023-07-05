package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Set;

/**
 * client-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public interface CcpsiServer<T> extends TwoPartyPto {
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
    SquareZ2Vector psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
