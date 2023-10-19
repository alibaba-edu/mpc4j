package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * Payable PSI client interface.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public interface PayablePsiClient<T> extends TwoPartyPto {
    /**
     * client initializes protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * client executes protocol.
     *
     * @param clientElementSet  client element set.
     * @param serverElementSize server element size.
     * @return intersection set.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
