package edu.alibaba.mpc4j.s2pc.pir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;

/**
 * index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public interface IdxPirServer extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param database    database.
     * @param maxBatchNum max batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException;

    /**
     * Server initializes the protocol.
     *
     * @param database database.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void init(NaiveDatabase database) throws MpcAbortException {
        init(database, 1);
    }

    /**
     * Server executes the protocol.
     *
     * @param batchNum batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir(int batchNum) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void pir() throws MpcAbortException {
        pir(1);
    }
}
