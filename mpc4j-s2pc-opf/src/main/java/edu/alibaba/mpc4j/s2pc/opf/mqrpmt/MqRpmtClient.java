package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * mqRPMT client.
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public interface MqRpmtClient extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementSet  max client element size.
     * @param serverElementSize max server element size.
     * @return client output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    boolean[] mqRpmt(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException;
}
