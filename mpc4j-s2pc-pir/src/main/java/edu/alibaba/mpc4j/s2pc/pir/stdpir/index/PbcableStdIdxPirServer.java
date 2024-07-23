package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirServer;

import java.util.List;

/**
 * index PIR server where the scheme supports batch query using probabilistic batch code (PBC) technique.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public interface PbcableStdIdxPirServer extends IdxPirServer {
    /**
     * Server initializes the protocol.
     *
     * @param serverKeys  server keys.
     * @param database    database.
     * @param maxBatchNum max batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(List<byte[]> serverKeys, NaiveDatabase database, int maxBatchNum) throws MpcAbortException;


    /**
     * Answers the query.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void answer() throws MpcAbortException;
}
