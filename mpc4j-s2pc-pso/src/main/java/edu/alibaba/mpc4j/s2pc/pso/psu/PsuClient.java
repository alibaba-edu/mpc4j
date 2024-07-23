package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU client.
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public interface PsuClient extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure abort.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementSet  client element set.
     * @param serverElementSize server element size.
     * @param elementByteLength element byte length.
     * @return client output.
     * @throws MpcAbortException the protocol failure abort.
     */
    PsuClientOutput psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException;
}
