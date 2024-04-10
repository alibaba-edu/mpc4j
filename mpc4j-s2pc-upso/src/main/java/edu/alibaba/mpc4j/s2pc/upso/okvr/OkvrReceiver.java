package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * OKVR receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public interface OkvrReceiver extends TwoPartyPto {
    /**
     * init the protocol.
     *
     * @param num           number of key-value pairs.
     * @param l             value bit length.
     * @param retrievalSize retrieval size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int num, int l, int retrievalSize) throws MpcAbortException;

    /**
     * Executes OKVR.
     *
     * @param keys retrieval keys.
     * @return receiver outputs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Map<ByteBuffer, byte[]> okvr(Set<ByteBuffer> keys) throws MpcAbortException;
}
