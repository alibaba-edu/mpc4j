package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.List;
import java.util.Map;

/**
 * batch index PIR client interface.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchIndexPirClient extends TwoPartyPto {

    /**
     * Client initializes the protocol.
     *
     * @param serverElementSize server element size.
     * @param elementBitLength  element bit length.
     * @param maxRetrievalSize  max retrieval size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param retrievalIndexList retrieval index list。
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Map<Integer, byte[]> pir(List<Integer> retrievalIndexList) throws MpcAbortException;
}
