package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.List;

/**
 * Single Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/8/10
 */
public interface SingleIndexPirClient extends TwoPartyPto {

    /**
     * Client initializes the protocol.
     *
     * @param indexPirParams    index PIR params.
     * @param serverElementSize database size.
     * @param elementBitLength  element bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength) throws MpcAbortException;

    /**
     * Client initializes the protocol.
     *
     * @param serverElementSize database size.
     * @param elementBitLength  element bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int serverElementSize, int elementBitLength) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param index index value.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] pir(int index) throws MpcAbortException;

    /**
     * Client generates key pair.
     *
     * @param serverElementSize server element size.
     * @param elementBitLength  element bit length.
     * @return public keys.
     */
    List<byte[]> clientSetup(int serverElementSize, int elementBitLength);

    /**
     * Client generates query.
     *
     * @param index retrieval index.
     * @return client query.
     */
    List<byte[]> generateQuery(int index);

    /**
     * Client decodes response.
     *
     * @param serverResponse server response.
     * @param index          retrieval index.
     * @return retrieval element.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] decodeResponse(List<byte[]> serverResponse, int index) throws MpcAbortException;

    /**
     * Client decodes response.
     *
     * @param serverResponse   server response.
     * @param index            retrieval index.
     * @param elementBitLength element bit length.
     * @return retrieval element.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] decodeResponse(List<byte[]> serverResponse, int index, int elementBitLength) throws MpcAbortException;

    /**
     * set default params.
     */
    void setDefaultParams();
}
