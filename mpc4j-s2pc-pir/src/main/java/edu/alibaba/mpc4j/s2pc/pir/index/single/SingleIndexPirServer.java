package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;

import java.util.List;

/**
 * Single Index PIR server.
 *
 * @author Liqiang Peng
 * @date 2022/8/10
 */
public interface SingleIndexPirServer extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param indexPirParams index PIR parameters.
     * @param database       database.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException;

    /**
     * Server initializes the protocol.
     *
     * @param database database。
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(NaiveDatabase database) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir() throws MpcAbortException;

    /**
     * get client query size.
     *
     * @return query size.
     */
    int getQuerySize();

    /**
     * server handle client public keys.
     *
     * @param clientPublicKeysPayload client public key payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException;

    /**
     * server setup database.
     *
     * @param database database.
     * @return BFV plaintexts in NTT form.
     */
    List<byte[][]> serverSetup(NaiveDatabase database);

    /**
     * server handle client query.
     *
     * @param clientQuery     client query.
     * @param encodedDatabase encoded database.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    List<byte[]> generateResponse(List<byte[]> clientQuery, List<byte[][]> encodedDatabase) throws MpcAbortException;

    /**
     * server handle client query.
     *
     * @param clientQuery client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException;

    /**
     * set default params.
     */
    void setDefaultParams();
}
