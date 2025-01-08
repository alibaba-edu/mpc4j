package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;

/**
 * client-specific preprocessing index PIR client where the scheme supports batch query using
 * probabilistic batch code (PBC) technique.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
public interface PbcCpIdxPirClient extends IdxPirClient {
    /**
     * Generates and sends the query.
     *
     * @param x queried input.
     */
    void query(int x);

    /**
     * Recovers the entry.
     *
     * @param x queried input.
     * @return entry.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] recover(int x) throws MpcAbortException;
}
