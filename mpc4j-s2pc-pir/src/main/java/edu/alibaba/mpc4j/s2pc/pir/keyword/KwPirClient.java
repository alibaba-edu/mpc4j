package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * Keyword PIR client interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirClient extends TwoPartyPto {

    /**
     * client initializes protocol.
     *
     * @param kwPirParams       keyword PIR params.
     * @param serverElementSize server element size.
     * @param maxRetrievalSize  max retrieval size.
     * @param valueByteLength   value byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(KwPirParams kwPirParams, int serverElementSize, int maxRetrievalSize, int valueByteLength)
        throws MpcAbortException;

    /**
     * client initializes protocol.
     *
     * @
     * @param maxRetrievalSize  max retrieval size.
     * @param serverElementSize server element size.
     * @param valueByteLength   value byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxRetrievalSize, int serverElementSize, int valueByteLength) throws MpcAbortException;

    /**
     * client executes protocol.
     *
     * @param retrievalKeySet retrieval key set.
     * @return key value map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Map<ByteBuffer, byte[]> pir(Set<ByteBuffer> retrievalKeySet) throws MpcAbortException;
}
