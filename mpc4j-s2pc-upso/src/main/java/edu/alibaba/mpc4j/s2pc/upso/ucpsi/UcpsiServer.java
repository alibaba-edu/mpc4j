package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Set;

/**
 * Unbalanced Circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public interface UcpsiServer<T> extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param serverElementSet     server element set.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Set<T> serverElementSet, int maxClientElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector psi() throws MpcAbortException;
}
