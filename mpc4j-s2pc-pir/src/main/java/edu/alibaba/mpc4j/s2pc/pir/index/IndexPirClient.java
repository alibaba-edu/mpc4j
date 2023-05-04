package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/8/10
 */
public interface IndexPirClient extends TwoPartyPto {
    /**
     * Client initializes the protocol.
     *
     * @param indexPirParams    index PIR params.
     * @param serverElementSize database size.
     * @param elementByteLength element byte length.
     */
    void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength);

    /**
     * Client initializes the protocol.
     *
     * @param serverElementSize database size.
     * @param elementByteLength element byte length.
     */
    void init(int serverElementSize, int elementByteLength);

    /**
     * Client executes the protocol.
     *
     * @param index index value.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] pir(int index) throws MpcAbortException;
}
