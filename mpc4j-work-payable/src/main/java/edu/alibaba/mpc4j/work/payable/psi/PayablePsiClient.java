package edu.alibaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Payable PSI client interface.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public interface PayablePsiClient extends TwoPartyPto {

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
    Set<ByteBuffer> payablePsi(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException;
}
