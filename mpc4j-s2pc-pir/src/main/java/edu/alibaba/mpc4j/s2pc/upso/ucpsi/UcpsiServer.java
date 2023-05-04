package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Unbalanced Circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public interface UcpsiServer extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param serverElementSet     server element set.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Set<ByteBuffer> serverElementSet, int maxClientElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector psi() throws MpcAbortException;
}
