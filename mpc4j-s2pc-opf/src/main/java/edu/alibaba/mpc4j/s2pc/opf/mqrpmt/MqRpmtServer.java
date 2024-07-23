package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * mqRPMT server.
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public interface MqRpmtServer extends TwoPartyPto {
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
     * @return server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ByteBuffer[] mqRpmt(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException;
}
