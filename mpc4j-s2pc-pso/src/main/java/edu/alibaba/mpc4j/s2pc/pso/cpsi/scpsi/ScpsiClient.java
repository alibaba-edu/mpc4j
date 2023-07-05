package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Set;

/**
 * server-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface ScpsiClient<T> extends TwoPartyPto {
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
    SquareZ2Vector psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
