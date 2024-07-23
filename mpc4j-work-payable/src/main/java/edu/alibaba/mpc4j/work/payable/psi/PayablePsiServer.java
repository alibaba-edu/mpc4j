package edu.alibaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Payable PSI server interface.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public interface PayablePsiServer extends TwoPartyPto {

    /**
     * server initializes protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * server executes protocol.
     *
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @return intersection set size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    int payablePsi(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException;
}
