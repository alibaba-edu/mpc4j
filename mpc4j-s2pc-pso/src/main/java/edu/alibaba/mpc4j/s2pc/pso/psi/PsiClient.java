package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI client interface
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public interface PsiClient<T> extends TwoPartyPto {
    /**
     * init protocol
     *
     * @param maxClientElementSize max size of elements of client
     * @param maxServerElementSize max size of elements of server
     * @throws MpcAbortException If protocol aborts
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * run protocol
     *
     * @param clientElementSet  the set of client's elements
     * @param serverElementSize the size of elements of server
     * @return set intersection
     * @throws MpcAbortException If protocol aborts
     */
    Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
