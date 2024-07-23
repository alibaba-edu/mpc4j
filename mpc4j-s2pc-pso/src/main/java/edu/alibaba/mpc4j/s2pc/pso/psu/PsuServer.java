package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU server.
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public interface PsuServer extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @param elementByteLength element byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength) throws MpcAbortException;
}
