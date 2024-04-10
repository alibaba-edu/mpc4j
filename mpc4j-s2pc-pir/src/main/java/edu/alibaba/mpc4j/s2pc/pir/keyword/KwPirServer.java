package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Keyword PIR server interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirServer extends TwoPartyPto {
    /**
     * server initializes protocol.
     *
     * @param kwPirParams      keyword PIR params.
     * @param keyValueMap      key value map.
     * @param maxRetrievalSize max retrieval size.
     * @param valueByteLength  value byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(KwPirParams kwPirParams, Map<ByteBuffer, byte[]> keyValueMap, int maxRetrievalSize,
              int valueByteLength) throws MpcAbortException;

    /**
     * server initializes protocol.
     *
     * @param keyValueMap      key value map.
     * @param maxRetrievalSize max retrieval size.
     * @param valueByteLength  value byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<ByteBuffer, byte[]> keyValueMap, int maxRetrievalSize, int valueByteLength)
        throws MpcAbortException;

    /**
     * server executes protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir() throws MpcAbortException;
}
