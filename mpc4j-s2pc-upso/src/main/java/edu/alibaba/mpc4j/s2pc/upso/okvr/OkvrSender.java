package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * OKVR sender.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public interface OkvrSender extends TwoPartyPto {
    /**
     * Generates the hint.
     *
     * @param keyValueMap   key-value map.
     * @param l             value bit length.
     * @param retrievalSize retrieval size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<ByteBuffer, byte[]> keyValueMap, int l, int retrievalSize) throws MpcAbortException;

    /**
     * Executes OKVR.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void okvr() throws MpcAbortException;
}
