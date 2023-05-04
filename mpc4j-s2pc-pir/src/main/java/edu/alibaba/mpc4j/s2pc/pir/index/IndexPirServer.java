package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;

/**
 * Index PIR server.
 *
 * @author Liqiang Peng
 * @date 2022/8/10
 */
public interface IndexPirServer extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param indexPirParams index PIR parameters.
     * @param database       database.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(IndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException;

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
}
