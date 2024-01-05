package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;

/**
 * batch index PIR server interface.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchIndexPirServer extends TwoPartyPto {

    /**
     * Server initializes the protocol.
     *
     * @param database         database.
     * @param maxRetrievalSize max retrieval size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir() throws MpcAbortException;
}
