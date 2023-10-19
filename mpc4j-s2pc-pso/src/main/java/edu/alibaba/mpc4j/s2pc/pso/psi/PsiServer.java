package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI server interface
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public interface PsiServer<T> extends TwoPartyPto {
    /**
     * init protocol
     *
     * @param maxServerElementSize max size of elements of server
     * @param maxClientElementSize max size of elements of client
     * @throws MpcAbortException If protocol aborts
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * run the protocol
     *
     * @param serverElementSet  the set of server's elements
     * @param clientElementSize the size of elements of client
     * @throws MpcAbortException If protocol aborts
     */
    void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
